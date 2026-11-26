package app.sukoon.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.sukoon.R
import app.sukoon.domain.model.ArabicFont

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

val Fraunces = FontFamily(
    Font(R.font.fraunces_regular, FontWeight.Normal),
    Font(R.font.fraunces_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
)

val AmiriQuran = FontFamily(Font(R.font.amiri_quran, FontWeight.Normal))

val Amiri = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold),
)

fun ArabicFont.family(): FontFamily = when (this) {
    ArabicFont.AMIRI_QURAN -> AmiriQuran
    ArabicFont.AMIRI -> Amiri
}

/** Material type scale: Fraunces for display & headlines, Inter for everything functional. */
val SukoonTypography = Typography(
    displayLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Normal, fontSize = 52.sp, lineHeight = 60.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Normal, fontSize = 42.sp, lineHeight = 50.sp, letterSpacing = (-0.25).sp),
    displaySmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 42.sp),
    headlineLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineSmall = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    bodyMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.2.sp),
    bodySmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.3.sp),
    labelLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelSmall = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp),
)

/** Reading styles that sit outside the Material scale. Scales come from user settings. */
@Immutable
data class ReadingTypography(
    val arabic: TextStyle,
    val transliteration: TextStyle,
    val translation: TextStyle,
    val reference: TextStyle,
    val eyebrow: TextStyle,
)

fun readingTypography(
    arabicFont: ArabicFont = ArabicFont.AMIRI_QURAN,
    arabicScale: Float = 1f,
    transliterationScale: Float = 1f,
    translationScale: Float = 1f,
) = ReadingTypography(
    arabic = TextStyle(
        fontFamily = arabicFont.family(),
        fontSize = (28 * arabicScale).sp,
        // Generous line height so stacked harakat never clip, at any font scale.
        lineHeight = 2.1.em,
        textDirection = TextDirection.Rtl,
        localeList = LocaleList("ar"),
        platformStyle = PlatformTextStyle(includeFontPadding = true),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    ),
    transliteration = TextStyle(
        fontFamily = Inter,
        fontStyle = FontStyle.Italic,
        fontSize = (15 * transliterationScale).sp,
        lineHeight = 1.6.em,
        letterSpacing = 0.1.sp,
    ),
    translation = TextStyle(
        fontFamily = Fraunces,
        fontSize = (18 * translationScale).sp,
        lineHeight = 1.55.em,
    ),
    reference = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 1.2.sp),
    eyebrow = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.6.sp),
)
