package app.sukoon.ui.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import app.sukoon.R
import app.sukoon.core.designsystem.theme.Radii
import app.sukoon.core.designsystem.theme.Spacing

fun Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

fun Context.openAppNotificationSettings(channelId: String? = null) {
    val intent = if (channelId != null) {
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
    } else {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Stable
class NotificationPermissionRequest internal constructor(
    private val start: (onGranted: () -> Unit) -> Unit,
) {
    /** Runs [onGranted] immediately if already allowed, otherwise walks the rationale → system prompt flow. */
    fun request(onGranted: () -> Unit) = start(onGranted)
}

/**
 * Rationale sheet before the system prompt, a snackbar-free soft decline, and a
 * "turn on in Settings" dialog once the permission is permanently denied.
 */
@Composable
fun rememberNotificationPermissionRequest(onDenied: () -> Unit = {}): NotificationPermissionRequest {
    val context = LocalContext.current
    var showRationale by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            pending?.invoke()
        } else {
            val activity = context.findActivity()
            val canAskAgain = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
            if (!canAskAgain) showSettings = true else onDenied()
        }
        pending = null
    }

    if (showRationale) {
        RationaleSheet(
            onAllow = {
                showRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onNotNow = {
                showRationale = false
                pending = null
                onDenied()
            },
        )
    }
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false; onDenied() },
            icon = { Icon(Icons.Outlined.NotificationsOff, null) },
            title = { Text(stringResource(R.string.perm_blocked_title)) },
            text = { Text(stringResource(R.string.perm_blocked_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showSettings = false
                    context.openAppNotificationSettings()
                }) { Text(stringResource(R.string.perm_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false; onDenied() }) { Text(stringResource(R.string.action_not_now)) }
            },
        )
    }

    return remember(context) {
        NotificationPermissionRequest { onGranted ->
            if (context.hasNotificationPermission()) {
                onGranted()
            } else {
                pending = onGranted
                showRationale = true
            }
        }
    }
}

@Composable
private fun RationaleSheet(onAllow: () -> Unit, onNotNow: () -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onNotNow, sheetState = state, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(Spacing.lg))
            Text(stringResource(R.string.perm_rationale_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                stringResource(R.string.perm_rationale_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xl))
            Button(onClick = onAllow, shape = RoundedCornerShape(Radii.pill), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.perm_allow))
            }
            TextButton(onClick = onNotNow, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.action_not_now))
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}
