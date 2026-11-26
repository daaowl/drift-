package app.sukoon.ui.lock

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sukoon.R
import app.sukoon.core.designsystem.component.GeometricOrnament
import app.sukoon.core.designsystem.theme.Spacing
import app.sukoon.core.designsystem.theme.SukoonTheme
import app.sukoon.core.feedback.rememberHaptics
import app.sukoon.domain.repository.SecurityRepository
import app.sukoon.security.BiometricAuth
import app.sukoon.ui.permission.findActivity
import app.sukoon.ui.settings.PIN_LENGTH
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockViewModel @Inject constructor(private val security: SecurityRepository) : ViewModel() {
    fun verify(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(security.verifyPin(pin)) }
    }
}

@Composable
fun LockScreen(
    biometricAllowed: Boolean,
    onUnlocked: () -> Unit,
    viewModel: LockViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context.findActivity() as? FragmentActivity
    var entry by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    var attempts by rememberSaveable { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val bioTitle = stringResource(R.string.lock_biometric_title)
    val bioSubtitle = stringResource(R.string.lock_biometric_subtitle)
    val usePin = stringResource(R.string.lock_use_pin)
    val canBiometric = biometricAllowed && activity != null && BiometricAuth.isAvailable(activity)

    val promptBiometric = {
        if (canBiometric && activity != null) {
            BiometricAuth.prompt(activity, bioTitle, bioSubtitle, usePin, onSuccess = onUnlocked, onFallback = {})
        }
    }
    LaunchedEffect(Unit) { promptBiometric() }

    val bioLabel = stringResource(R.string.lock_use_biometric)
    val bioKey: (@Composable () -> Unit)? = if (canBiometric) {
        {
            IconButton(onClick = { promptBiometric() }, modifier = Modifier.size(72.dp)) {
                Icon(Icons.Outlined.Fingerprint, contentDescription = bioLabel, modifier = Modifier.size(28.dp))
            }
        }
    } else {
        null
    }

    // Swallow back so the content underneath cannot be reached.
    BackHandler { activity?.moveTaskToBack(true) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(enabled = false) {},
    ) {
        GeometricOrnament(SukoonTheme.extended.ornament, Modifier.fillMaxSize(), cell = 72.dp)
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(Spacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Lock, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(Spacing.lg))
            Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                if (error) stringResource(R.string.lock_wrong_pin, attempts) else stringResource(R.string.lock_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xl))
            Box(Modifier.offset { IntOffset(shake.value.toInt(), 0) }) {
                PinPad(
                    value = entry,
                    length = PIN_LENGTH,
                    onValueChange = { v ->
                        entry = v
                        error = false
                        if (v.length == PIN_LENGTH) {
                            viewModel.verify(v) { ok ->
                                if (ok) {
                                    haptics.confirm()
                                    onUnlocked()
                                } else {
                                    attempts++
                                    error = true
                                    entry = ""
                                    haptics.complete()
                                    scope.launch {
                                        for (x in listOf(24f, -24f, 16f, -16f, 0f)) {
                                            shake.animateTo(x, spring(stiffness = Spring.StiffnessHigh))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    extraKey = bioKey,
                )
            }
        }
    }
}

/** Large, accessible numeric keypad with dot indicators. */
@Composable
fun PinPad(
    value: String,
    length: Int,
    onValueChange: (String) -> Unit,
    extraKey: (@Composable () -> Unit)? = null,
) {
    val filledLabel = stringResource(R.string.pin_progress, value.length, length)
    Row(
        Modifier.semantics { contentDescription = filledLabel },
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        repeat(length) { i ->
            Box(
                Modifier
                    .size(14.dp)
                    .background(
                        if (i < value.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                        CircleShape,
                    ),
            )
        }
    }
    Spacer(Modifier.height(Spacing.xxl))
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                row.forEach { d -> Key(d) { if (value.length < length) onValueChange(value + d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) { extraKey?.invoke() }
            Key("0") { if (value.length < length) onValueChange(value + "0") }
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                IconButton(onClick = { if (value.isNotEmpty()) onValueChange(value.dropLast(1)) }, modifier = Modifier.size(72.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = stringResource(R.string.cd_delete_digit))
                }
            }
        }
    }
}

@Composable
private fun Key(digit: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(digit, style = MaterialTheme.typography.headlineMedium)
    }
}
