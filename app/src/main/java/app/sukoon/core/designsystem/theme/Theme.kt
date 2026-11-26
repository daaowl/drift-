package app.sukoon.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.sukoon.domain.model.AccentColor
import app.sukoon.domain.model.ThemeMode
import app.sukoon.domain.model.UserSettings

@Immutable
data class SukoonExtendedColors(
    val ornament: Color,
    val cardWash: Color,
    val divider: Color,
    val success: Color,
    val isDark: Boolean,
)

val LocalExtendedColors = staticCompositionLocalOf {
    SukoonExtendedColors(Palette.GoldDark, Palette.Ink850, Palette.Ink700, Palette.SageDark, true)
}
val LocalReadingTypography = staticCompositionLocalOf { readingTypography() }

object SukoonTheme {
    val extended: SukoonExtendedColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current
    val reading: ReadingTypography
        @Composable @ReadOnlyComposable get() = LocalReadingTypography.current
}

fun darkScheme(accent: AccentColor, amoled: Boolean): ColorScheme {
    val a = accent.tones().onDark
    val bg = if (amoled) Palette.AmoledBlack else Palette.Ink900
    val surface = if (amoled) Palette.AmoledBlack else Palette.Ink900
    val container = if (amoled) Palette.AmoledSurface else Palette.Ink850
    val containerHigh = if (amoled) Palette.AmoledSurfaceHigh else Palette.Ink800
    return darkColorScheme(
        primary = a,
        onPrimary = Palette.Ink950,
        primaryContainer = a.copy(alpha = 0.18f).compositeOver(container),
        onPrimaryContainer = a,
        secondary = Palette.SageDark,
        onSecondary = Palette.Ink950,
        secondaryContainer = Palette.SageDark.copy(alpha = 0.16f).compositeOver(container),
        onSecondaryContainer = Palette.SageDark,
        tertiary = Palette.SkyDark,
        onTertiary = Palette.Ink950,
        background = bg,
        onBackground = Palette.Cream,
        surface = surface,
        onSurface = Palette.Cream,
        surfaceVariant = containerHigh,
        onSurfaceVariant = Palette.CreamMuted,
        surfaceContainerLowest = bg,
        surfaceContainerLow = container,
        surfaceContainer = container,
        surfaceContainerHigh = containerHigh,
        surfaceContainerHighest = if (amoled) Palette.Ink850 else Palette.Ink700,
        outline = Palette.Ink600,
        outlineVariant = if (amoled) Palette.Ink800 else Palette.Ink700,
        error = Palette.ErrorDark,
        onError = Palette.Ink950,
        inverseSurface = Palette.Cream,
        inverseOnSurface = Palette.Ink900,
        inversePrimary = accent.tones().onLight,
        scrim = Color.Black,
    )
}

fun lightScheme(accent: AccentColor): ColorScheme {
    val a = accent.tones().onLight
    return lightColorScheme(
        primary = a,
        onPrimary = Palette.Parchment50,
        primaryContainer = a.copy(alpha = 0.14f).compositeOver(Palette.Parchment50),
        onPrimaryContainer = a,
        secondary = Palette.SageLight,
        onSecondary = Palette.Parchment50,
        secondaryContainer = Palette.SageLight.copy(alpha = 0.14f).compositeOver(Palette.Parchment50),
        onSecondaryContainer = Palette.SageLight,
        tertiary = Palette.SkyLight,
        onTertiary = Palette.Parchment50,
        background = Palette.Parchment100,
        onBackground = Palette.Umber,
        surface = Palette.Parchment100,
        onSurface = Palette.Umber,
        surfaceVariant = Palette.Parchment200,
        onSurfaceVariant = Palette.UmberMuted,
        surfaceContainerLowest = Palette.Parchment50,
        surfaceContainerLow = Palette.Parchment50,
        surfaceContainer = Palette.Parchment50,
        surfaceContainerHigh = Palette.Parchment200,
        surfaceContainerHighest = Palette.Parchment300,
        outline = Palette.Parchment400,
        outlineVariant = Palette.Parchment300,
        error = Palette.ErrorLight,
        onError = Palette.Parchment50,
        inverseSurface = Palette.Ink900,
        inverseOnSurface = Palette.Cream,
        inversePrimary = accent.tones().onDark,
        scrim = Color.Black,
    )
}

private fun Color.compositeOver(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
        alpha = 1f,
    )
}

@Composable
fun isAppInDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK, ThemeMode.AMOLED -> true
}

@Composable
fun SukoonTheme(
    settings: UserSettings = UserSettings(),
    content: @Composable () -> Unit,
) {
    val dark = isAppInDarkTheme(settings.themeMode)
    val amoled = settings.themeMode == ThemeMode.AMOLED
    val context = LocalContext.current
    val dynamicAvailable = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val scheme = when {
        dynamicAvailable && dark -> dynamicDarkColorScheme(context).let {
            if (amoled) it.copy(background = Color.Black, surface = Color.Black, surfaceContainerLowest = Color.Black) else it
        }
        dynamicAvailable -> dynamicLightColorScheme(context)
        dark -> darkScheme(settings.accent, amoled)
        else -> lightScheme(settings.accent)
    }

    val extended = SukoonExtendedColors(
        ornament = scheme.primary.copy(alpha = if (dark) 0.10f else 0.12f),
        cardWash = scheme.surfaceContainer,
        divider = scheme.outlineVariant,
        success = if (dark) Palette.SageDark else Palette.SageLight,
        isDark = dark,
    )

    val reading = remember(settings.arabicFont, settings.arabicScale, settings.transliterationScale, settings.translationScale) {
        readingTypography(settings.arabicFont, settings.arabicScale, settings.transliterationScale, settings.translationScale)
    }

    CompositionLocalProvider(
        LocalExtendedColors provides extended,
        LocalReadingTypography provides reading,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = SukoonTypography,
            shapes = SukoonShapes,
            content = content,
        )
    }
}
