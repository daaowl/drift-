package app.sukoon.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

enum class AccentColor { GOLD, SAGE, ROSE, SKY, TERRACOTTA }

enum class ArabicFont { AMIRI_QURAN, AMIRI }

enum class HapticStrength { OFF, LIGHT, MEDIUM, STRONG }

enum class LaunchDestination { HOME, FAVORITES, LAST_READ }

enum class DailySource { ALL, SECTION }

enum class WidgetMode { DAILY, PINNED }

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val accent: AccentColor = AccentColor.GOLD,

    val arabicScale: Float = 1f,
    val transliterationScale: Float = 1f,
    val translationScale: Float = 1f,
    val showArabic: Boolean = true,
    val showTransliteration: Boolean = true,
    val showTranslation: Boolean = true,
    val arabicFont: ArabicFont = ArabicFont.AMIRI_QURAN,
    val keepScreenOn: Boolean = false,

    val counterHaptic: HapticStrength = HapticStrength.MEDIUM,
    val counterSound: Boolean = false,
    val counterAutoReset: Boolean = true,
    val counterVibrateOnComplete: Boolean = true,

    val launchDestination: LaunchDestination = LaunchDestination.HOME,
    val resumeLastRead: Boolean = true,
    val lastReadDuaId: String? = null,

    val dailyEnabled: Boolean = true,
    val dailyMinuteOfDay: Int = 9 * 60,
    val dailySource: DailySource = DailySource.ALL,
    val dailySectionId: String? = null,
    val morningEnabled: Boolean = false,
    val morningMinuteOfDay: Int = 7 * 60,
    val eveningEnabled: Boolean = false,
    val eveningMinuteOfDay: Int = 18 * 60,

    val widgetMode: WidgetMode = WidgetMode.DAILY,
    val widgetPinnedDuaId: String? = null,

    val appLockEnabled: Boolean = false,
    val biometricUnlock: Boolean = true,
    val hideFromRecents: Boolean = false,
    val secureScreen: Boolean = false,

    val onboardingDone: Boolean = false,
) {
    companion object {
        const val MIN_SCALE = 0.8f
        const val MAX_SCALE = 2f
    }
}
