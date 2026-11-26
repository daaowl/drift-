package app.sukoon.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sukoon.core.designsystem.component.ArabicText
import app.sukoon.core.designsystem.component.Eyebrow
import app.sukoon.core.designsystem.component.GeometricOrnament
import app.sukoon.core.designsystem.component.RepeatBadge
import app.sukoon.core.designsystem.component.StarGlyph
import app.sukoon.core.designsystem.component.sharedBoundsFor
import app.sukoon.core.designsystem.theme.Radii
import app.sukoon.core.designsystem.theme.SectionTints
import app.sukoon.core.designsystem.theme.Spacing
import app.sukoon.core.designsystem.theme.SukoonTheme
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.DuaKind
import app.sukoon.domain.model.UserSettings

/**
 * One dua, typeset like a page of a prayer book. Used by the pager and by the tablet detail pane.
 */
@Composable
fun DuaPage(
    dua: Dua,
    settings: UserSettings,
    modifier: Modifier = Modifier,
    sectionTitle: String? = null,
    animateIn: Boolean = true,
    shareTitle: Boolean = true,
) {
    val tint = SectionTints.of(dua.sectionId)
    var shown by rememberSaveable(dua.id) { mutableStateOf(!animateIn) }
    LaunchedEffect(dua.id) { shown = true }

    Box(modifier.fillMaxSize()) {
        GeometricOrnament(
            color = tint.copy(alpha = 0.05f),
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            cell = 72.dp,
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = Spacing.readingMaxWidth).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (sectionTitle != null) {
                    Eyebrow(sectionTitle, color = tint)
                    Spacer(Modifier.height(Spacing.sm))
                }
                Text(
                    dua.title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = (if (shareTitle) Modifier.sharedBoundsFor("dua-${dua.id}") else Modifier)
                        .semantics { heading() },
                )
                if (dua.recitationNote != null && dua.kind == DuaKind.DUA) {
                    Spacer(Modifier.height(Spacing.md))
                    RecitationNote(dua.recitationNote, dua.repeatCount)
                }

                Spacer(Modifier.height(Spacing.xl))

                if (dua.kind == DuaKind.GUIDANCE) {
                    GuidanceBody(dua)
                } else {
                    SelectionContainer {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Reveal(settings.showArabic && dua.arabic.isNotBlank() && shown, delay = 0) {
                                ArabicText(dua.arabic)
                            }
                            Reveal(settings.showTransliteration && dua.transliteration.isNotBlank() && shown, delay = 120) {
                                Text(
                                    dua.transliteration,
                                    style = SukoonTheme.reading.transliteration,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = Spacing.lg),
                                )
                            }
                            Ornament()
                            Reveal(settings.showTranslation && shown, delay = 220) {
                                Text(
                                    dua.translation,
                                    style = SukoonTheme.reading.translation.copy(localeList = LocaleList("en")),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.xl))
                Text(
                    dua.reference.uppercase(),
                    style = SukoonTheme.reading.reference,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.huge))
            }
        }
    }
}

@Composable
private fun Reveal(visible: Boolean, delay: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600, delayMillis = delay)) + expandVertically(tween(400, delayMillis = delay)),
        exit = fadeOut(tween(200)) + shrinkVertically(tween(250)),
    ) { content() }
}

@Composable
private fun Ornament() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        HorizontalDivider(Modifier.widthIn(max = 72.dp).weight(1f, fill = false), color = SukoonTheme.extended.divider)
        Spacer(Modifier.size(Spacing.md))
        StarGlyph(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), Modifier.size(10.dp))
        Spacer(Modifier.size(Spacing.md))
        HorizontalDivider(Modifier.widthIn(max = 72.dp).weight(1f, fill = false), color = SukoonTheme.extended.divider)
    }
}

@Composable
private fun RecitationNote(note: String, repeat: Int?) {
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(Radii.pill))
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(14.dp))
        Spacer(Modifier.size(Spacing.xs))
        Text(note, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, textAlign = TextAlign.Center)
        if (repeat != null) {
            Spacer(Modifier.size(Spacing.sm))
            RepeatBadge(repeat)
        }
    }
}

@Composable
private fun GuidanceBody(dua: Dua) {
    Text(
        dua.translation,
        style = SukoonTheme.reading.translation,
        textAlign = TextAlign.Center,
    )
    if (dua.recitationNote != null) {
        Spacer(Modifier.height(Spacing.xl))
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.large)
                .padding(Spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.sm))
                Eyebrow(androidx.compose.ui.res.stringResource(app.sukoon.R.string.guidance_label))
            }
            Spacer(Modifier.height(Spacing.sm))
            Text(dua.recitationNote, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
