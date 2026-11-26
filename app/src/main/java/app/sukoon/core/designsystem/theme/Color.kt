package app.sukoon.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import app.sukoon.domain.model.AccentColor

/** Raw palette. Screens never use these directly; they read MaterialTheme / SukoonTheme tokens. */
object Palette {
    // Ink (dark)
    val Ink950 = Color(0xFF0A0E18)
    val Ink900 = Color(0xFF0E1320)
    val Ink850 = Color(0xFF131A2A)
    val Ink800 = Color(0xFF182033)
    val Ink700 = Color(0xFF212A40)
    val Ink600 = Color(0xFF2E3850)
    val Ink400 = Color(0xFF5A6480)

    // Parchment (light)
    val Parchment50 = Color(0xFFFBF7EE)
    val Parchment100 = Color(0xFFF6F0E3)
    val Parchment200 = Color(0xFFEFE6D3)
    val Parchment300 = Color(0xFFE4D8BF)
    val Parchment400 = Color(0xFFD6CAB0)

    // Text
    val Cream = Color(0xFFECE6D8)
    val CreamMuted = Color(0xFFA8A496)
    val Umber = Color(0xFF1B1A17)
    val UmberMuted = Color(0xFF5F5849)

    // Accents (dark-tone, light-tone)
    val GoldDark = Color(0xFFD4AF6A)
    val GoldLight = Color(0xFF8A6A2E)
    val SageDark = Color(0xFF9DB89A)
    val SageLight = Color(0xFF4F6B4C)
    val RoseDark = Color(0xFFE0A3A0)
    val RoseLight = Color(0xFF94504C)
    val SkyDark = Color(0xFF9DBEDC)
    val SkyLight = Color(0xFF3D6688)
    val TerracottaDark = Color(0xFFE2A27F)
    val TerracottaLight = Color(0xFF9A5434)

    val ErrorDark = Color(0xFFF2B8B5)
    val ErrorLight = Color(0xFFA23B33)

    val AmoledBlack = Color(0xFF000000)
    val AmoledSurface = Color(0xFF07090E)
    val AmoledSurfaceHigh = Color(0xFF10141E)
}

data class AccentTones(val onDark: Color, val onLight: Color)

fun AccentColor.tones(): AccentTones = when (this) {
    AccentColor.GOLD -> AccentTones(Palette.GoldDark, Palette.GoldLight)
    AccentColor.SAGE -> AccentTones(Palette.SageDark, Palette.SageLight)
    AccentColor.ROSE -> AccentTones(Palette.RoseDark, Palette.RoseLight)
    AccentColor.SKY -> AccentTones(Palette.SkyDark, Palette.SkyLight)
    AccentColor.TERRACOTTA -> AccentTones(Palette.TerracottaDark, Palette.TerracottaLight)
}

/** Each of the ten sections carries a subtle hue used for its card wash and icon. */
object SectionTints {
    private val tints = mapOf(
        "tranquility" to Color(0xFF7FA7C9),
        "solace" to Color(0xFFB39DDB),
        "success" to Color(0xFFD4AF6A),
        "knowledge" to Color(0xFF8DB596),
        "love" to Color(0xFFE09A9A),
        "forgiveness" to Color(0xFF8FC1C1),
        "protection" to Color(0xFF9FA8C9),
        "health" to Color(0xFFA9C98A),
        "sustenance" to Color(0xFFD9B27C),
        "straight_path" to Color(0xFFC9B48F),
    )

    fun of(sectionId: String): Color = tints[sectionId] ?: Palette.GoldDark
}
