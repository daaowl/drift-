package app.sukoon.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.sukoon.R
import app.sukoon.core.designsystem.theme.Motion
import app.sukoon.core.designsystem.theme.Radii
import app.sukoon.core.designsystem.theme.SectionTints
import app.sukoon.core.designsystem.theme.Spacing
import app.sukoon.core.designsystem.theme.SukoonTheme
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.DuaKind
import app.sukoon.domain.model.Section

@Composable
fun ArabicText(text: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE, color: Color = MaterialTheme.colorScheme.onSurface) {
    Text(
        text = text,
        style = SukoonTheme.reading.arabic,
        color = color,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Text(text.uppercase(), style = SukoonTheme.reading.eyebrow, color = color, modifier = modifier)
}

@Composable
fun SectionCard(
    section: Section,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = SectionTints.of(section.id)
    val count = pluralStringResource(R.plurals.dua_count, section.duaCount, section.duaCount)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .sharedBoundsFor("section-${section.id}")
            .heightIn(min = 148.dp)
            .testTag("section_card"),
    ) {
        Box {
            Box(
                Modifier
                    .matchParentSize()
                    .background(tint.copy(alpha = if (SukoonTheme.extended.isDark) 0.08f else 0.10f)),
            )
            GeometricOrnament(tint.copy(alpha = 0.10f), Modifier.matchParentSize(), cell = 44.dp)
            Column(Modifier.padding(Spacing.lg)) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(SectionIcons.of(section.id), contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(Spacing.lg))
                Text(section.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    section.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(count, style = MaterialTheme.typography.labelMedium, color = tint)
            }
        }
    }
}

@Composable
fun DuaRow(
    dua: Dua,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    showSection: String? = null,
    selected: Boolean = false,
) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        label = "rowColor",
    )
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = container,
        modifier = modifier
            .fillMaxWidth()
            .sharedBoundsFor("dua-${dua.id}")
            .testTag("dua_row"),
    ) {
        Row(
            Modifier.padding(start = Spacing.lg, top = Spacing.md, bottom = Spacing.md, end = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                if (showSection != null) {
                    Eyebrow(showSection, color = SectionTints.of(dua.sectionId))
                    Spacer(Modifier.height(Spacing.xxs))
                }
                Text(dua.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    if (dua.kind == DuaKind.GUIDANCE) dua.translation else dua.transliteration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dua.reference, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (dua.repeatCount != null) {
                        Spacer(Modifier.width(Spacing.sm))
                        RepeatBadge(dua.repeatCount)
                    }
                }
            }
            FavoriteButton(dua.isFavorite, onToggleFavorite)
        }
    }
}

@Composable
fun RepeatBadge(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(Radii.pill))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Repeat, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        Spacer(Modifier.width(Spacing.xxs))
        Text(stringResource(R.string.repeat_times, count), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
fun FavoriteButton(isFavorite: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(if (isFavorite) 1.12f else 1f, Motion.bouncy(), label = "favScale")
    val state = stringResource(if (isFavorite) R.string.state_saved else R.string.state_not_saved)
    IconButton(
        onClick = onToggle,
        modifier = modifier.semantics { stateDescription = state },
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(if (isFavorite) R.string.cd_unfavorite else R.string.cd_favorite),
            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.scale(scale),
        )
    }
}

@Composable
fun SettingsGroupTitle(text: String, modifier: Modifier = Modifier) {
    Eyebrow(text, modifier.padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.xl, bottom = Spacing.sm))
}

@Composable
fun NavRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.gutter, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailing != null) trailing() else Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
