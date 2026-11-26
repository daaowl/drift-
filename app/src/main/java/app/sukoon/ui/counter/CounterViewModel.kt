package app.sukoon.ui.counter

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.sukoon.domain.model.CounterProgress
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.LoadState
import app.sukoon.domain.repository.DuaRepository
import app.sukoon.domain.repository.SettingsRepository
import app.sukoon.domain.usecase.CounterUseCase
import app.sukoon.ui.navigation.CounterRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class CounterContent(
    val dua: Dua,
    val progress: CounterProgress,
    /** 0 means open-ended counting. */
    val target: Int,
)

enum class CounterEvent { TICK, COMPLETE }

@HiltViewModel
class CounterViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val duas: DuaRepository,
    private val counter: CounterUseCase,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val duaId = savedState.toRoute<CounterRoute>().duaId
    private val retry = MutableStateFlow(0)
    private val mutex = Mutex()

    /** In-memory source of truth while the screen is open, so rapid taps never lose a count. */
    private val local = MutableStateFlow<CounterProgress?>(null)

    init {
        viewModelScope.launch {
            local.value = runCatching { counter.observe(duaId).first() }.getOrDefault(CounterProgress(duaId, 0, 0))
        }
    }

    val state: StateFlow<LoadState<CounterContent>> = retry.flatMapLatest {
        combine(duas.observeDua(duaId), local.filterNotNull()) { dua, progress ->
            if (dua == null) LoadState.Error(NoSuchElementException(duaId))
            else LoadState.Success(CounterContent(dua, progress, dua.repeatCount ?: 0))
        }.catch { emit(LoadState.Error(it)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    private val _events = Channel<CounterEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun increment() {
        val target = (state.value as? LoadState.Success)?.value?.target ?: return
        viewModelScope.launch {
            mutex.withLock {
                val current = local.value ?: return@withLock
                val autoReset = settings.settings.first().counterAutoReset
                val (next, completed) = counter.increment(current, target, autoReset)
                local.value = next
                _events.send(if (completed) CounterEvent.COMPLETE else CounterEvent.TICK)
            }
        }
    }

    fun reset() {
        viewModelScope.launch {
            mutex.withLock {
                val current = local.value ?: return@withLock
                counter.reset(current)
                local.value = current.copy(count = 0, completedRounds = 0)
            }
        }
    }

    fun retry() { retry.value++ }
}
