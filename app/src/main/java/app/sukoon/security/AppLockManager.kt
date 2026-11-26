package app.sukoon.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import app.sukoon.di.ApplicationScope
import app.sukoon.domain.repository.SecurityRepository
import app.sukoon.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Locks the app whenever the process goes to the background while App lock is on.
 * Starts locked so a cold start always asks for authentication.
 */
@Singleton
class AppLockManager @Inject constructor(
    settings: SettingsRepository,
    security: SecurityRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _locked = MutableStateFlow(true)

    /** null until settings load; true when the lock screen must be shown. */
    val requiresUnlock: StateFlow<Boolean?> = combine(settings.settings, security.hasPin, _locked) { s, hasPin, locked ->
        s.appLockEnabled && hasPin && locked
    }.stateIn(scope, SharingStarted.Eagerly, null)

    val biometricAllowed: StateFlow<Boolean> = settings.settings.map { it.biometricUnlock }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    private var suspendNextLock = false

    fun start() {
        scope.launch(kotlinx.coroutines.Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    if (suspendNextLock) suspendNextLock = false else _locked.value = true
                }
            })
        }
    }

    fun unlock() {
        _locked.value = false
    }

    /** Called before launching a system picker (backup file, etc.) so returning does not re-lock. */
    fun allowExternalActivity() {
        suspendNextLock = true
    }
}
