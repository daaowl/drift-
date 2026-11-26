package app.sukoon.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.LoadState
import app.sukoon.domain.model.Section
import app.sukoon.domain.repository.DuaRepository
import app.sukoon.domain.repository.SettingsRepository
import app.sukoon.domain.usecase.GetDuaOfTheDayUseCase
import app.sukoon.domain.usecase.ObserveSectionsUseCase
import app.sukoon.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeContent(
    val sections: List<Section>,
    val duaOfTheDay: Dua?,
    val continueReading: Dua?,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeSections: ObserveSectionsUseCase,
    private val duaRepository: DuaRepository,
    private val settingsRepository: SettingsRepository,
    private val duaOfTheDay: GetDuaOfTheDayUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {

    private val retry = MutableStateFlow(0)

    private val settings = settingsRepository.settings

    private val daily = settings
        .distinctUntilChanged { a, b -> a.dailySource == b.dailySource && a.dailySectionId == b.dailySectionId }
        .mapLatest { duaOfTheDay(it, LocalDate.now()) }
        .flatMapLatest { picked -> picked?.let { duaRepository.observeDua(it.id) } ?: flowOf(null) }

    private val lastRead = settings
        .map { if (it.resumeLastRead) it.lastReadDuaId else null }
        .distinctUntilChanged()
        .flatMapLatest { id -> id?.let { duaRepository.observeDua(it) } ?: flowOf(null) }

    val state: StateFlow<LoadState<HomeContent>> = retry.flatMapLatest {
        combine(observeSections(), daily, lastRead) { sections, d, last ->
            LoadState.Success(HomeContent(sections, d, last)) as LoadState<HomeContent>
        }.catch { emit(LoadState.Error(it)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoadState.Loading)

    fun retry() { retry.value++ }

    fun toggleFavorite(dua: Dua) {
        viewModelScope.launch { toggleFavorite.invoke(dua) }
    }
}
