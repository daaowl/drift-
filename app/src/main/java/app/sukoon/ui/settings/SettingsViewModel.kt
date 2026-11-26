package app.sukoon.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sukoon.domain.model.Section
import app.sukoon.domain.model.UserSettings
import app.sukoon.domain.repository.BackupRepository
import app.sukoon.domain.repository.ImportResult
import app.sukoon.domain.repository.SecurityRepository
import app.sukoon.domain.repository.SettingsRepository
import app.sukoon.domain.repository.UserDataRepository
import app.sukoon.domain.usecase.ObserveSectionsUseCase
import app.sukoon.security.AppLockManager
import app.sukoon.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SettingsMessage {
    data object Exported : SettingsMessage
    data class Imported(val favorites: Int, val collections: Int) : SettingsMessage
    data object ImportUnreadable : SettingsMessage
    data object ImportTooNew : SettingsMessage
    data object Failed : SettingsMessage
    data object Cleared : SettingsMessage
    data object Reset : SettingsMessage
    data object NeedPin : SettingsMessage
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val security: SecurityRepository,
    private val userData: UserDataRepository,
    private val backup: BackupRepository,
    private val appLock: AppLockManager,
    private val widgetUpdater: WidgetUpdater,
    observeSections: ObserveSectionsUseCase,
) : ViewModel() {

    val hasPin: StateFlow<Boolean> = security.hasPin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val sections: StateFlow<List<Section>> = observeSections()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = Channel<SettingsMessage>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun updateWidget(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            settingsRepository.update(transform)
            widgetUpdater.refresh()
        }
    }

    fun setAppLock(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && !hasPin.value) {
                _messages.send(SettingsMessage.NeedPin)
                return@launch
            }
            appLock.unlock()
            settingsRepository.update { it.copy(appLockEnabled = enabled) }
        }
    }

    fun removePin() {
        viewModelScope.launch { security.clearPin() }
    }

    fun beforeExternalActivity() = appLock.allowExternalActivity()

    fun export(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching {
                val out = context.contentResolver.openOutputStream(uri, "wt") ?: error("No stream")
                backup.export(out)
            }.isSuccess
            _messages.send(if (ok) SettingsMessage.Exported else SettingsMessage.Failed)
        }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val input = context.contentResolver.openInputStream(uri) ?: error("No stream")
                backup.import(input)
            }.getOrElse { ImportResult.Failure(ImportResult.Reason.UNREADABLE) }
            _messages.send(
                when (result) {
                    is ImportResult.Success -> SettingsMessage.Imported(result.favorites, result.collections)
                    is ImportResult.Failure -> when (result.reason) {
                        ImportResult.Reason.UNREADABLE -> SettingsMessage.ImportUnreadable
                        ImportResult.Reason.UNSUPPORTED_VERSION -> SettingsMessage.ImportTooNew
                    }
                },
            )
        }
    }

    fun clearData() {
        viewModelScope.launch {
            runCatching {
                userData.clearAll()
                settingsRepository.update { it.copy(lastReadDuaId = null, widgetPinnedDuaId = null) }
                widgetUpdater.refresh()
            }
            _messages.send(SettingsMessage.Cleared)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
            widgetUpdater.refresh()
            _messages.send(SettingsMessage.Reset)
        }
    }
}
