package app.sukoon.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sukoon.domain.model.UserSettings
import app.sukoon.domain.repository.DuaRepository
import app.sukoon.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Error(val throwable: Throwable) : MainUiState
    data class Ready(val settings: UserSettings) : MainUiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val duas: DuaRepository,
    private val settingsRepository: SettingsRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {

    private sealed interface Seed {
        data object Pending : Seed
        data object Done : Seed
        data class Failed(val t: Throwable) : Seed
    }

    private val seed = MutableStateFlow<Seed>(Seed.Pending)

    val uiState: StateFlow<MainUiState> = combine(seed, settingsRepository.settings) { s, settings ->
        when (s) {
            Seed.Pending -> MainUiState.Loading
            is Seed.Failed -> MainUiState.Error(s.t)
            Seed.Done -> MainUiState.Ready(settings)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)

    private val _pendingDuaId = MutableStateFlow<String?>(savedState[KEY_PENDING])
    val pendingDuaId: StateFlow<String?> = _pendingDuaId.asStateFlow()

    /** Cold-start launch routing runs once per task, surviving rotation and process death. */
    var launchRoutingDone: Boolean
        get() = savedState[KEY_LAUNCH_DONE] ?: false
        set(value) { savedState[KEY_LAUNCH_DONE] = value }

    init { seed() }

    fun seed() {
        seed.value = Seed.Pending
        viewModelScope.launch {
            seed.value = runCatching { duas.ensureSeeded() }
                .fold(onSuccess = { Seed.Done }, onFailure = { Seed.Failed(it) })
        }
    }

    fun openDua(duaId: String?) {
        if (duaId.isNullOrBlank()) return
        savedState[KEY_PENDING] = duaId
        _pendingDuaId.value = duaId
    }

    fun consumePendingDua() {
        savedState[KEY_PENDING] = null
        _pendingDuaId.value = null
    }

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.update { it.copy(onboardingDone = true) } }
    }

    private companion object {
        const val KEY_PENDING = "pending_dua"
        const val KEY_LAUNCH_DONE = "launch_done"
    }
}
