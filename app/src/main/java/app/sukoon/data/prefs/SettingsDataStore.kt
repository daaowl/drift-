package app.sukoon.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.sukoon.domain.model.AccentColor
import app.sukoon.domain.model.ArabicFont
import app.sukoon.domain.model.DailySource
import app.sukoon.domain.model.HapticStrength
import app.sukoon.domain.model.LaunchDestination
import app.sukoon.domain.model.ThemeMode
import app.sukoon.domain.model.UserSettings
import app.sukoon.domain.model.WidgetMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsDataStore @Inject constructor(
    private val store: DataStore<Preferences>,
) {
    private object K {
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val accent = stringPreferencesKey("accent")
        val arabicScale = floatPreferencesKey("arabic_scale")
        val translitScale = floatPreferencesKey("translit_scale")
        val translationScale = floatPreferencesKey("translation_scale")
        val showArabic = booleanPreferencesKey("show_arabic")
        val showTranslit = booleanPreferencesKey("show_translit")
        val showTranslation = booleanPreferencesKey("show_translation")
        val arabicFont = stringPreferencesKey("arabic_font")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val counterHaptic = stringPreferencesKey("counter_haptic")
        val counterSound = booleanPreferencesKey("counter_sound")
        val counterAutoReset = booleanPreferencesKey("counter_auto_reset")
        val counterVibrate = booleanPreferencesKey("counter_vibrate")
        val launchDestination = stringPreferencesKey("launch_destination")
        val resumeLastRead = booleanPreferencesKey("resume_last_read")
        val lastReadDuaId = stringPreferencesKey("last_read_dua_id")
        val dailyEnabled = booleanPreferencesKey("daily_enabled")
        val dailyMinute = intPreferencesKey("daily_minute")
        val dailySource = stringPreferencesKey("daily_source")
        val dailySectionId = stringPreferencesKey("daily_section_id")
        val morningEnabled = booleanPreferencesKey("morning_enabled")
        val morningMinute = intPreferencesKey("morning_minute")
        val eveningEnabled = booleanPreferencesKey("evening_enabled")
        val eveningMinute = intPreferencesKey("evening_minute")
        val widgetMode = stringPreferencesKey("widget_mode")
        val widgetPinned = stringPreferencesKey("widget_pinned")
        val appLock = booleanPreferencesKey("app_lock")
        val biometric = booleanPreferencesKey("biometric")
        val hideRecents = booleanPreferencesKey("hide_recents")
        val secureScreen = booleanPreferencesKey("secure_screen")
        val onboardingDone = booleanPreferencesKey("onboarding_done")

        val contentVersion = intPreferencesKey("content_version")
        val pinHash = stringPreferencesKey("pin_hash")
        val pinSalt = stringPreferencesKey("pin_salt")
    }

    private val safeData: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val settings: Flow<UserSettings> = safeData.map { it.toSettings() }

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        store.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs.write(updated)
        }
    }

    suspend fun resetPreservingState() {
        store.edit { prefs ->
            val current = prefs.toSettings()
            prefs.write(
                UserSettings(
                    onboardingDone = current.onboardingDone,
                    lastReadDuaId = current.lastReadDuaId,
                    appLockEnabled = current.appLockEnabled,
                    biometricUnlock = current.biometricUnlock,
                ),
            )
        }
    }

    suspend fun contentVersion(): Int = safeData.first()[K.contentVersion] ?: 0
    suspend fun setContentVersion(version: Int) {
        store.edit { it[K.contentVersion] = version }
    }

    val pinHash: Flow<Pair<String, String>?> = safeData.map { p ->
        val hash = p[K.pinHash]
        val salt = p[K.pinSalt]
        if (hash != null && salt != null) hash to salt else null
    }

    suspend fun setPin(hash: String, salt: String) {
        store.edit {
            it[K.pinHash] = hash
            it[K.pinSalt] = salt
        }
    }

    suspend fun clearPin() {
        store.edit {
            it.remove(K.pinHash)
            it.remove(K.pinSalt)
            it[K.appLock] = false
        }
    }

    private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
        this?.let { v -> enumValues<E>().firstOrNull { it.name == v } } ?: default

    private fun Preferences.toSettings(): UserSettings {
        val d = UserSettings()
        return UserSettings(
            themeMode = this[K.themeMode].toEnum(d.themeMode),
            dynamicColor = this[K.dynamicColor] ?: d.dynamicColor,
            accent = this[K.accent].toEnum(d.accent),
            arabicScale = (this[K.arabicScale] ?: d.arabicScale).coerceIn(UserSettings.MIN_SCALE, UserSettings.MAX_SCALE),
            transliterationScale = (this[K.translitScale] ?: d.transliterationScale).coerceIn(UserSettings.MIN_SCALE, UserSettings.MAX_SCALE),
            translationScale = (this[K.translationScale] ?: d.translationScale).coerceIn(UserSettings.MIN_SCALE, UserSettings.MAX_SCALE),
            showArabic = this[K.showArabic] ?: d.showArabic,
            showTransliteration = this[K.showTranslit] ?: d.showTransliteration,
            showTranslation = this[K.showTranslation] ?: d.showTranslation,
            arabicFont = this[K.arabicFont].toEnum(d.arabicFont),
            keepScreenOn = this[K.keepScreenOn] ?: d.keepScreenOn,
            counterHaptic = this[K.counterHaptic].toEnum(d.counterHaptic),
            counterSound = this[K.counterSound] ?: d.counterSound,
            counterAutoReset = this[K.counterAutoReset] ?: d.counterAutoReset,
            counterVibrateOnComplete = this[K.counterVibrate] ?: d.counterVibrateOnComplete,
            launchDestination = this[K.launchDestination].toEnum(d.launchDestination),
            resumeLastRead = this[K.resumeLastRead] ?: d.resumeLastRead,
            lastReadDuaId = this[K.lastReadDuaId],
            dailyEnabled = this[K.dailyEnabled] ?: d.dailyEnabled,
            dailyMinuteOfDay = this[K.dailyMinute] ?: d.dailyMinuteOfDay,
            dailySource = this[K.dailySource].toEnum(d.dailySource),
            dailySectionId = this[K.dailySectionId],
            morningEnabled = this[K.morningEnabled] ?: d.morningEnabled,
            morningMinuteOfDay = this[K.morningMinute] ?: d.morningMinuteOfDay,
            eveningEnabled = this[K.eveningEnabled] ?: d.eveningEnabled,
            eveningMinuteOfDay = this[K.eveningMinute] ?: d.eveningMinuteOfDay,
            widgetMode = this[K.widgetMode].toEnum(d.widgetMode),
            widgetPinnedDuaId = this[K.widgetPinned],
            appLockEnabled = this[K.appLock] ?: d.appLockEnabled,
            biometricUnlock = this[K.biometric] ?: d.biometricUnlock,
            hideFromRecents = this[K.hideRecents] ?: d.hideFromRecents,
            secureScreen = this[K.secureScreen] ?: d.secureScreen,
            onboardingDone = this[K.onboardingDone] ?: d.onboardingDone,
        )
    }

    private fun MutablePreferences.write(s: UserSettings) {
        this[K.themeMode] = s.themeMode.name
        this[K.dynamicColor] = s.dynamicColor
        this[K.accent] = s.accent.name
        this[K.arabicScale] = s.arabicScale
        this[K.translitScale] = s.transliterationScale
        this[K.translationScale] = s.translationScale
        this[K.showArabic] = s.showArabic
        this[K.showTranslit] = s.showTransliteration
        this[K.showTranslation] = s.showTranslation
        this[K.arabicFont] = s.arabicFont.name
        this[K.keepScreenOn] = s.keepScreenOn
        this[K.counterHaptic] = s.counterHaptic.name
        this[K.counterSound] = s.counterSound
        this[K.counterAutoReset] = s.counterAutoReset
        this[K.counterVibrate] = s.counterVibrateOnComplete
        this[K.launchDestination] = s.launchDestination.name
        this[K.resumeLastRead] = s.resumeLastRead
        setOrRemove(K.lastReadDuaId, s.lastReadDuaId)
        this[K.dailyEnabled] = s.dailyEnabled
        this[K.dailyMinute] = s.dailyMinuteOfDay
        this[K.dailySource] = s.dailySource.name
        setOrRemove(K.dailySectionId, s.dailySectionId)
        this[K.morningEnabled] = s.morningEnabled
        this[K.morningMinute] = s.morningMinuteOfDay
        this[K.eveningEnabled] = s.eveningEnabled
        this[K.eveningMinute] = s.eveningMinuteOfDay
        this[K.widgetMode] = s.widgetMode.name
        setOrRemove(K.widgetPinned, s.widgetPinnedDuaId)
        this[K.appLock] = s.appLockEnabled
        this[K.biometric] = s.biometricUnlock
        this[K.hideRecents] = s.hideFromRecents
        this[K.secureScreen] = s.secureScreen
        this[K.onboardingDone] = s.onboardingDone
    }

    private fun MutablePreferences.setOrRemove(key: Preferences.Key<String>, value: String?) {
        if (value == null) remove(key) else this[key] = value
    }
}
