package app.sukoon.ui.counter

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sukoon.R
import app.sukoon.core.designsystem.component.ArabicText
import app.sukoon.core.designsystem.component.ErrorState
import app.sukoon.core.designsystem.component.GeometricOrnament
import app.sukoon.core.designsystem.component.LoadingState
import app.sukoon.core.designsystem.component.SukoonTopBar
import app.sukoon.core.designsystem.theme.Motion
import app.sukoon.core.designsystem.theme.Spacing
import app.sukoon.core.designsystem.theme.SukoonTheme
import app.sukoon.core.feedback.rememberHaptics
import app.sukoon.core.feedback.rememberTickSound
import app.sukoon.domain.model.HapticStrength
import app.sukoon.domain.model.LoadState
import app.sukoon.ui.main.LocalAppEnvironment

@Composable
fun CounterScreen(
    onBack: () -> Unit,
    viewModel: CounterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = LocalAppEnvironment.current.settings
    val haptics = rememberHaptics()
    val sound = rememberTickSound()

    LaunchedEffect(viewModel, settings.counterHaptic, settings.counterSound, settings.counterVibrateOnComplete) {
        viewModel.events.collect { event ->
            when (event) {
                CounterEvent.TICK -> {
                    haptics.tick(settings.counterHaptic)
                    if (settings.counterSound) sound.tick()
                }
                CounterEvent.COMPLETE -> {
                    if (settings.counterVibrateOnComplete) haptics.complete() else haptics.tick(settings.counterHaptic)
                    if (settings.counterSound) sound.done()
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        SukoonTopBar(
            title = (state as? LoadState.Success)?.value?.dua?.title.orEmpty(),
            onBack = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        ) {
            if (state is LoadState.Success) ResetAction(onReset = viewModel::reset)
        }
        when (val s = state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(
                icon = Icons.Outlined.ErrorOutline,
                title = stringResource(R.string.error_dua_title),
                body = stringResource(R.string.error_dua_body),
                onRetry = viewModel::retry,
            )
            is LoadState.Success -> CounterBody(
                content = s.value,
                hapticsOff = settings.counterHaptic == HapticStrength.OFF,
                onTap = viewModel::increment,
            )
        }
    }
}

@Composable
private fun ResetAction(onReset: () -> Unit) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { confirm = true }) {
        Icon(Icons.Outlined.RestartAlt, contentDescription = stringResource(R.string.counter_reset))
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.counter_reset_title)) },
            text = { Text(stringResource(R.string.counter_reset_body)) },
            confirmButton = {
                TextButton(onClick = { confirm = false; onReset() }) { Text(stringResource(R.string.counter_reset)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
fun CounterBody(content: CounterContent, hapticsOff: Boolean, onTap: () -> Unit) {
    val target = content.target
    val count = content.progress.count
    val progress by animateFloatAsState(
        if (target > 0) (count.toFloat() / target).coerceIn(0f, 1f) else 0f,
        Motion.gentle(),
        label = "progress",
    )
    var pressed by remember { mutableStateOf(0) }
    val pulse by animateFloatAsState(if (pressed % 2 == 0) 1f else 0.97f, Motion.bouncy(), label = "pulse")
    val ringColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val cd = if (target > 0) stringResource(R.string.counter_cd_target, count, target) else stringResource(R.string.counter_cd_open, count)
    val tapLabel = stringResource(R.string.counter_tap)

    Box(
        Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = tapLabel,
            ) {
                pressed++
                onTap()
            }
            .testTag("counter_tap_area")
            .semantics { contentDescription = cd },
    ) {
        GeometricOrnament(SukoonTheme.extended.ornament, Modifier.matchParentSize(), cell = 80.dp)
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(Spacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (content.dua.arabic.isNotBlank()) {
                ArabicText(content.dua.arabic, maxLines = 3, modifier = Modifier.widthIn(max = Spacing.readingMaxWidth))
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth(0.8f)
                    .aspectRatio(1f)
                    .scale(pulse),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(trackColor, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                    if (target > 0) {
                        drawArc(ringColor, -90f, 360f * progress, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedContent(
                        targetState = count,
                        transitionSpec = { (fadeIn(tween(140)) + scaleIn(initialScale = 0.85f)) togetherWith fadeOut(tween(90)) },
                        label = "count",
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    ) { n ->
                        Text("$n", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    if (target > 0) {
                        Text(
                            stringResource(R.string.counter_of_target, target),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(Spacing.xl))
            Text(
                pluralStringResource(R.plurals.counter_rounds, content.progress.completedRounds, content.progress.completedRounds),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(if (hapticsOff) R.string.counter_hint_silent else R.string.counter_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
