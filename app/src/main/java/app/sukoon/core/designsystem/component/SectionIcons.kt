package app.sukoon.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

object SectionIcons {
    fun of(sectionId: String): ImageVector = when (sectionId) {
        "tranquility" -> Icons.Outlined.Spa
        "solace" -> Icons.Outlined.VolunteerActivism
        "success" -> Icons.Outlined.AutoAwesome
        "knowledge" -> Icons.Outlined.LocalLibrary
        "love" -> Icons.Outlined.FavoriteBorder
        "forgiveness" -> Icons.Outlined.WaterDrop
        "protection" -> Icons.Outlined.Shield
        "health" -> Icons.Outlined.HealthAndSafety
        "sustenance" -> Icons.Outlined.Grain
        else -> Icons.Outlined.Explore
    }
}
