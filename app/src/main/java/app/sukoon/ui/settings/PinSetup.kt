package app.sukoon.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import app.sukoon.R
import app.sukoon.core.designsystem.component.SukoonTopBar
import app.sukoon.core.designsystem.theme.Spacing
import app.sukoon.core.feedback.rememberHaptics
import app.sukoon.domain.repository.SecurityRepository
import app.sukoon.domain.repository.SettingsRepository
import app.sukoon.security.AppLockManager
import app.sukoon.ui.lock.PinPad
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PinSetupViewModel @Inject constructor(
    private val security: SecurityRepository,
    private val settings: SettingsRepository,
    private val appLock: AppLockManager,
) : ViewModel() {
    private val _done = Channel<Unit>(Channel.CONFLATED)
    val done = _done.receiveAsFlow()

    fun save(pin: String) {
        viewModelScope.launch {
            security.setPin(pin)
            appLock.unlock()
            settings.update { it.copy(appLockEnabled = true) }
            _done.send(Unit)
        }
    }
}

@Composable
fun PinSetupScreen(onDone: () -> Unit, viewModel: PinSetupViewModel = hiltViewModel()) {
    var first by rememberSaveable { mutableStateOf<String?>(null) }
    var entry by rememberSaveable { mutableStateOf("") }
    var mismatch by rememberSaveable { mutableStateOf(false) }
    val haptics = rememberHaptics()

    LaunchedEffect(viewModel) { viewModel.done.collect { onDone() } }

    Column(Modifier.fillMaxSize()) {
        SukoonTopBar(stringResource(R.string.pin_setup_title), onBack = onDone, modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
        Column(
            Modifier.fillMaxSize().padding(Spacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(if (first == null) R.string.pin_enter_new else R.string.pin_confirm),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                stringResource(if (mismatch) R.string.pin_mismatch else R.string.pin_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = if (mismatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xl))
            PinPad(
                value = entry,
                length = PIN_LENGTH,
                onValueChange = { v ->
                    entry = v
                    mismatch = false
                    if (v.length == PIN_LENGTH) {
                        val f = first
                        if (f == null) {
                            first = v
                            entry = ""
                        } else if (f == v) {
                            haptics.confirm()
                            viewModel.save(v)
                        } else {
                            haptics.complete()
                            mismatch = true
                            first = null
                            entry = ""
                        }
                    }
                },
            )
        }
    }
}

const val PIN_LENGTH = 4
