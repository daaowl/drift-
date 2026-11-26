package app.sukoon.ui.settings

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sukoon.R
import app.sukoon.core.designsystem.component.NavRow
import app.sukoon.core.designsystem.component.SettingsGroupTitle
import app.sukoon.core.designsystem.theme.Spacing
import app.sukoon.core.designsystem.theme.SukoonTheme
import app.sukoon.core.designsystem.theme.isAppInDarkTheme
import app.sukoon.core.designsystem.theme.tones
import app.sukoon.domain.model.AccentColor
import app.sukoon.domain.model.ArabicFont
import app.sukoon.domain.model.DailySource
import app.sukoon.domain.model.HapticStrength
import app.sukoon.domain.model.LaunchDestination
import app.sukoon.domain.model.ThemeMode
import app.sukoon.domain.model.UserSettings
import app.sukoon.domain.model.WidgetMode
import app.sukoon.notifications.NotificationChannels
import app.sukoon.ui.main.LocalAppEnvironment
import app.sukoon.ui.permission.openAppNotificationSettings
import app.sukoon.ui.permission.rememberNotificationPermissionRequest
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onSetupPin: () -> Unit,
    onAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val env = LocalAppEnvironment.current
    val s = env.settings
    val hasPin by viewModel.hasPin.collectAsStateWithLifecycle()
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    val msgExported = stringResource(R.string.data_exported)
    val msgImportedFmt = stringResource(R.string.data_imported)
    val msgUnreadable = stringResource(R.string.data_import_unreadable)
    val msgTooNew = stringResource(R.string.data_import_too_new)
    val msgFailed = stringResource(R.string.data_failed)
    val msgCleared = stringResource(R.string.data_cleared)
    val msgReset = stringResource(R.string.settings_reset_done)
    val msgDenied = stringResource(R.string.perm_denied_snackbar)

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { m ->
            when (m) {
                SettingsMessage.Exported -> env.snackbar.showSnackbar(msgExported)
                is SettingsMessage.Imported -> env.snackbar.showSnackbar(String.format(msgImportedFmt, m.favorites, m.collections))
                SettingsMessage.ImportUnreadable -> env.snackbar.showSnackbar(msgUnreadable)
                SettingsMessage.ImportTooNew -> env.snackbar.showSnackbar(msgTooNew)
                SettingsMessage.Failed -> env.snackbar.showSnackbar(msgFailed)
                SettingsMessage.Cleared -> env.snackbar.showSnackbar(msgCleared)
                SettingsMessage.Reset -> env.snackbar.showSnackbar(msgReset)
                SettingsMessage.NeedPin -> onSetupPin()
            }
        }
    }

    val scope = rememberCoroutineScope()
    val permission = rememberNotificationPermissionRequest(onDenied = { scope.launch { env.snackbar.showSnackbar(msgDenied) } })
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::export)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::import)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
            Text(
                stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier
                    .padding(horizontal = Spacing.gutter, vertical = Spacing.xl)
                    .semantics { heading() },
            )

            // ---------- Appearance ----------
            SettingsGroupTitle(stringResource(R.string.settings_appearance))
            ChoiceRow(
                title = stringResource(R.string.settings_theme),
                current = s.themeMode,
                choices = listOf(
                    Choice(ThemeMode.SYSTEM, stringResource(R.string.theme_system)),
                    Choice(ThemeMode.LIGHT, stringResource(R.string.theme_light)),
                    Choice(ThemeMode.DARK, stringResource(R.string.theme_dark)),
                    Choice(ThemeMode.AMOLED, stringResource(R.string.theme_amoled)),
                ),
                onSelect = { v -> viewModel.update { it.copy(themeMode = v) } },
            )
            val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            SwitchRow(
                title = stringResource(R.string.settings_dynamic_color),
                subtitle = stringResource(if (dynamicSupported) R.string.settings_dynamic_color_desc else R.string.settings_dynamic_color_unsupported),
                checked = s.dynamicColor && dynamicSupported,
                enabled = dynamicSupported,
                onCheckedChange = { v -> viewModel.update { it.copy(dynamicColor = v) } },
            )
            Text(
                stringResource(R.string.settings_accent),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.md),
            )
            val dark = isAppInDarkTheme(s.themeMode)
            val accents = AccentColor.entries
            AccentPicker(
                options = accents.map { a -> (if (dark) a.tones().onDark else a.tones().onLight) to accentLabel(a) },
                selectedIndex = accents.indexOf(s.accent),
                enabled = !(s.dynamicColor && dynamicSupported),
                onSelect = { i -> viewModel.update { it.copy(accent = accents[i]) } },
            )

            // ---------- Reading ----------
            SettingsGroupTitle(stringResource(R.string.settings_reading))
            SliderRow(
                title = stringResource(R.string.settings_arabic_size),
                value = s.arabicScale,
                range = UserSettings.MIN_SCALE..UserSettings.MAX_SCALE,
                onValueChangeFinished = { v -> viewModel.update { it.copy(arabicScale = v) } },
                valueLabel = { "${(it * 100).roundToInt()}%" },
                preview = { scale ->
                    Text(
                        stringResource(R.string.settings_arabic_preview),
                        style = SukoonTheme.reading.arabic.copy(fontSize = SukoonTheme.reading.arabic.fontSize * (scale / s.arabicScale)),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                    )
                },
            )
            SliderRow(
                title = stringResource(R.string.settings_translit_size),
                value = s.transliterationScale,
                range = UserSettings.MIN_SCALE..UserSettings.MAX_SCALE,
                onValueChangeFinished = { v -> viewModel.update { it.copy(transliterationScale = v) } },
                valueLabel = { "${(it * 100).roundToInt()}%" },
                preview = { scale ->
                    Text(
                        stringResource(R.string.settings_translit_preview),
                        style = SukoonTheme.reading.transliteration.copy(fontSize = SukoonTheme.reading.transliteration.fontSize * (scale / s.transliterationScale)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                    )
                },
            )
            SliderRow(
                title = stringResource(R.string.settings_translation_size),
                value = s.translationScale,
                range = UserSettings.MIN_SCALE..UserSettings.MAX_SCALE,
                onValueChangeFinished = { v -> viewModel.update { it.copy(translationScale = v) } },
                valueLabel = { "${(it * 100).roundToInt()}%" },
                preview = { scale ->
                    Text(
                        stringResource(R.string.settings_translation_preview),
                        style = SukoonTheme.reading.translation.copy(fontSize = SukoonTheme.reading.translation.fontSize * (scale / s.translationScale)),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.sm),
                    )
                },
            )
            SwitchRow(stringResource(R.string.reader_show_arabic), s.showArabic, { v ->
                viewModel.update { it.copy(showArabic = v || (!it.showTransliteration && !it.showTranslation)) }
            })
            SwitchRow(stringResource(R.string.reader_show_transliteration), s.showTransliteration, { v ->
                viewModel.update { it.copy(showTransliteration = v || (!it.showArabic && !it.showTranslation)) }
            })
            SwitchRow(stringResource(R.string.reader_show_translation), s.showTranslation, { v ->
                viewModel.update { it.copy(showTranslation = v || (!it.showArabic && !it.showTransliteration)) }
            })
            ChoiceRow(
                title = stringResource(R.string.settings_arabic_font),
                current = s.arabicFont,
                choices = listOf(
                    Choice(ArabicFont.AMIRI_QURAN, stringResource(R.string.font_amiri_quran)),
                    Choice(ArabicFont.AMIRI, stringResource(R.string.font_amiri)),
                ),
                onSelect = { v -> viewModel.update { it.copy(arabicFont = v) } },
            )
            SwitchRow(
                title = stringResource(R.string.settings_keep_screen_on),
                subtitle = stringResource(R.string.settings_keep_screen_on_desc),
                checked = s.keepScreenOn,
                onCheckedChange = { v -> viewModel.update { it.copy(keepScreenOn = v) } },
            )

            // ---------- Counter ----------
            SettingsGroupTitle(stringResource(R.string.settings_counter))
            ChoiceRow(
                title = stringResource(R.string.settings_haptics),
                current = s.counterHaptic,
                choices = listOf(
                    Choice(HapticStrength.OFF, stringResource(R.string.haptic_off)),
                    Choice(HapticStrength.LIGHT, stringResource(R.string.haptic_light)),
                    Choice(HapticStrength.MEDIUM, stringResource(R.string.haptic_medium)),
                    Choice(HapticStrength.STRONG, stringResource(R.string.haptic_strong)),
                ),
                onSelect = { v -> viewModel.update { it.copy(counterHaptic = v) } },
            )
            SwitchRow(stringResource(R.string.settings_counter_sound), s.counterSound, { v -> viewModel.update { it.copy(counterSound = v) } })
            SwitchRow(
                title = stringResource(R.string.settings_counter_auto_reset),
                subtitle = stringResource(R.string.settings_counter_auto_reset_desc),
                checked = s.counterAutoReset,
                onCheckedChange = { v -> viewModel.update { it.copy(counterAutoReset = v) } },
            )
            SwitchRow(
                stringResource(R.string.settings_counter_vibrate),
                s.counterVibrateOnComplete,
                { v -> viewModel.update { it.copy(counterVibrateOnComplete = v) } },
            )

            // ---------- Behaviour ----------
            SettingsGroupTitle(stringResource(R.string.settings_behavior))
            ChoiceRow(
                title = stringResource(R.string.settings_launch),
                current = s.launchDestination,
                choices = listOf(
                    Choice(LaunchDestination.HOME, stringResource(R.string.launch_home)),
                    Choice(LaunchDestination.FAVORITES, stringResource(R.string.launch_favorites)),
                    Choice(LaunchDestination.LAST_READ, stringResource(R.string.launch_last_read)),
                ),
                onSelect = { v -> viewModel.update { it.copy(launchDestination = v) } },
            )
            SwitchRow(
                title = stringResource(R.string.settings_resume),
                subtitle = stringResource(R.string.settings_resume_desc),
                checked = s.resumeLastRead,
                onCheckedChange = { v -> viewModel.update { it.copy(resumeLastRead = v, lastReadDuaId = if (v) it.lastReadDuaId else null) } },
            )

            // ---------- Reminders ----------
            SettingsGroupTitle(stringResource(R.string.settings_reminders))
            SwitchRow(
                title = stringResource(R.string.settings_daily),
                subtitle = stringResource(R.string.settings_daily_desc),
                checked = s.dailyEnabled,
                onCheckedChange = { v ->
                    if (v) permission.request { viewModel.update { it.copy(dailyEnabled = true) } }
                    else viewModel.update { it.copy(dailyEnabled = false) }
                },
            )
            TimeRow(stringResource(R.string.settings_daily_time), s.dailyMinuteOfDay, s.dailyEnabled) { m ->
                viewModel.update { it.copy(dailyMinuteOfDay = m) }
            }
            val sectionChoices = listOf(Choice<String?>(null, stringResource(R.string.daily_source_all))) +
                sections.map { Choice<String?>(it.id, it.title) }
            ChoiceRow(
                title = stringResource(R.string.settings_daily_source),
                current = if (s.dailySource == DailySource.ALL) null else s.dailySectionId,
                choices = sectionChoices,
                onSelect = { id ->
                    viewModel.updateWidget {
                        it.copy(dailySource = if (id == null) DailySource.ALL else DailySource.SECTION, dailySectionId = id)
                    }
                },
            )
            SwitchRow(
                title = stringResource(R.string.settings_morning),
                checked = s.morningEnabled,
                onCheckedChange = { v ->
                    if (v) permission.request { viewModel.update { it.copy(morningEnabled = true) } }
                    else viewModel.update { it.copy(morningEnabled = false) }
                },
            )
            TimeRow(stringResource(R.string.settings_morning_time), s.morningMinuteOfDay, s.morningEnabled) { m ->
                viewModel.update { it.copy(morningMinuteOfDay = m) }
            }
            SwitchRow(
                title = stringResource(R.string.settings_evening),
                checked = s.eveningEnabled,
                onCheckedChange = { v ->
                    if (v) permission.request { viewModel.update { it.copy(eveningEnabled = true) } }
                    else viewModel.update { it.copy(eveningEnabled = false) }
                },
            )
            TimeRow(stringResource(R.string.settings_evening_time), s.eveningMinuteOfDay, s.eveningEnabled) { m ->
                viewModel.update { it.copy(eveningMinuteOfDay = m) }
            }

            // ---------- Notifications (system channels) ----------
            SettingsGroupTitle(stringResource(R.string.settings_notifications))
            NavRow(
                title = stringResource(R.string.channel_daily),
                subtitle = stringResource(R.string.settings_channel_desc),
                onClick = { context.openAppNotificationSettings(NotificationChannels.DAILY) },
            )
            NavRow(
                title = stringResource(R.string.channel_morning),
                subtitle = stringResource(R.string.settings_channel_desc),
                onClick = { context.openAppNotificationSettings(NotificationChannels.MORNING) },
            )
            NavRow(
                title = stringResource(R.string.channel_evening),
                subtitle = stringResource(R.string.settings_channel_desc),
                onClick = { context.openAppNotificationSettings(NotificationChannels.EVENING) },
            )

            // ---------- Widget ----------
            SettingsGroupTitle(stringResource(R.string.settings_widget))
            ChoiceRow(
                title = stringResource(R.string.settings_widget_shows),
                current = s.widgetMode,
                choices = listOf(
                    Choice(WidgetMode.DAILY, stringResource(R.string.widget_mode_daily)),
                    Choice(WidgetMode.PINNED, stringResource(R.string.widget_mode_pinned)),
                ),
                onSelect = { v -> viewModel.updateWidget { it.copy(widgetMode = v) } },
            )
            if (s.widgetMode == WidgetMode.PINNED && s.widgetPinnedDuaId == null) {
                Text(
                    stringResource(R.string.settings_widget_pin_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.gutter),
                )
            }

            // ---------- Privacy ----------
            SettingsGroupTitle(stringResource(R.string.settings_privacy))
            SwitchRow(
                title = stringResource(R.string.settings_app_lock),
                subtitle = stringResource(if (hasPin) R.string.settings_app_lock_desc else R.string.settings_app_lock_needs_pin),
                checked = s.appLockEnabled && hasPin,
                onCheckedChange = viewModel::setAppLock,
            )
            SwitchRow(
                title = stringResource(R.string.settings_biometric),
                checked = s.biometricUnlock,
                enabled = s.appLockEnabled && hasPin,
                onCheckedChange = { v -> viewModel.update { it.copy(biometricUnlock = v) } },
            )
            NavRow(
                title = stringResource(if (hasPin) R.string.settings_change_pin else R.string.settings_set_pin),
                onClick = onSetupPin,
            )
            if (hasPin) {
                NavRow(title = stringResource(R.string.settings_remove_pin), onClick = viewModel::removePin)
            }
            SwitchRow(
                title = stringResource(R.string.settings_hide_recents),
                subtitle = stringResource(R.string.settings_hide_recents_desc),
                checked = s.hideFromRecents,
                onCheckedChange = { v -> viewModel.update { it.copy(hideFromRecents = v) } },
            )
            SwitchRow(
                title = stringResource(R.string.settings_secure_screen),
                subtitle = stringResource(R.string.settings_secure_screen_desc),
                checked = s.secureScreen,
                onCheckedChange = { v -> viewModel.update { it.copy(secureScreen = v) } },
            )

            // ---------- Data ----------
            SettingsGroupTitle(stringResource(R.string.settings_data))
            NavRow(
                title = stringResource(R.string.data_export),
                subtitle = stringResource(R.string.data_export_desc),
                onClick = {
                    viewModel.beforeExternalActivity()
                    exportLauncher.launch("sukoon-backup-${LocalDate.now()}.json")
                },
            )
            NavRow(
                title = stringResource(R.string.data_import),
                subtitle = stringResource(R.string.data_import_desc),
                onClick = {
                    viewModel.beforeExternalActivity()
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                },
            )
            NavRow(
                title = stringResource(R.string.data_clear),
                subtitle = stringResource(R.string.data_clear_desc),
                onClick = { confirmClear = true },
            )

            // ---------- Language ----------
            SettingsGroupTitle(stringResource(R.string.settings_language))
            LanguageRow()

            // ---------- About ----------
            SettingsGroupTitle(stringResource(R.string.settings_about))
            NavRow(title = stringResource(R.string.about_title), onClick = onAbout)
            HorizontalDivider(Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.md), color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(
                title = stringResource(R.string.settings_reset),
                subtitle = stringResource(R.string.settings_reset_desc),
                onClick = { confirmReset = true },
            )
            Spacer(Modifier.height(Spacing.xxl))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = stringResource(R.string.data_clear_title),
            body = stringResource(R.string.data_clear_body),
            confirm = stringResource(R.string.data_clear_confirm),
            onConfirm = { confirmClear = false; viewModel.clearData() },
            onDismiss = { confirmClear = false },
        )
    }
    if (confirmReset) {
        ConfirmDialog(
            title = stringResource(R.string.settings_reset_title),
            body = stringResource(R.string.settings_reset_body),
            confirm = stringResource(R.string.settings_reset_confirm),
            onConfirm = { confirmReset = false; viewModel.resetToDefaults() },
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
private fun accentLabel(a: AccentColor): String = stringResource(
    when (a) {
        AccentColor.GOLD -> R.string.accent_gold
        AccentColor.SAGE -> R.string.accent_sage
        AccentColor.ROSE -> R.string.accent_rose
        AccentColor.SKY -> R.string.accent_sky
        AccentColor.TERRACOTTA -> R.string.accent_terracotta
    },
)

@Composable
private fun LanguageRow() {
    val current = AppCompatDelegate.getApplicationLocales().toLanguageTags().ifBlank { "" }
    ChoiceRow(
        title = stringResource(R.string.settings_app_language),
        current = current.substringBefore('-'),
        choices = listOf(
            Choice("", stringResource(R.string.language_system)),
            Choice("en", stringResource(R.string.language_en)),
            Choice("ur", stringResource(R.string.language_ur)),
        ),
        onSelect = { tag ->
            AppCompatDelegate.setApplicationLocales(
                if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
            )
        },
    )
}

@Composable
private fun ConfirmDialog(title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
