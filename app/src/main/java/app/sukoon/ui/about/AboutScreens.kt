package app.sukoon.ui.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.RawRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.sukoon.BuildConfig
import app.sukoon.R
import app.sukoon.core.designsystem.component.ErrorState
import app.sukoon.core.designsystem.component.GeometricOrnament
import app.sukoon.core.designsystem.component.LoadingState
import app.sukoon.core.designsystem.component.NavRow
import app.sukoon.core.designsystem.component.SettingsGroupTitle
import app.sukoon.core.designsystem.component.SukoonTopBar
import app.sukoon.core.designsystem.theme.Radii
import app.sukoon.core.designsystem.theme.Spacing
import app.sukoon.core.designsystem.theme.SukoonTheme
import app.sukoon.ui.navigation.DocumentRoute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AboutScreen(onBack: () -> Unit, onOpenDocument: (String) -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        SukoonTopBar(stringResource(R.string.about_title), onBack = onBack, modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .padding(Spacing.gutter)
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(Radii.xl)),
                contentAlignment = Alignment.Center,
            ) {
                GeometricOrnament(SukoonTheme.extended.ornament, Modifier.matchParentSize(), cell = 56.dp)
                Column(Modifier.padding(Spacing.xxl), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painterResource(R.drawable.ic_launcher_monochrome),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp),
                    )
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
                    Text(
                        stringResource(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                SettingsGroupTitle(stringResource(R.string.about_source))
                Text(
                    stringResource(R.string.about_source_body),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = Spacing.gutter),
                )
                NavRow(
                    title = stringResource(R.string.about_nanima),
                    subtitle = stringResource(R.string.about_nanima_url),
                    onClick = { context.openUrl("https://www.nanima.co.za") },
                )
                SettingsGroupTitle(stringResource(R.string.about_app))
                NavRow(stringResource(R.string.about_changelog), onClick = { onOpenDocument(DocumentRoute.CHANGELOG) })
                NavRow(stringResource(R.string.about_licenses), onClick = { onOpenDocument(DocumentRoute.LICENSES) })
                NavRow(stringResource(R.string.about_privacy), onClick = { onOpenDocument(DocumentRoute.PRIVACY) })
                NavRow(stringResource(R.string.about_rate), subtitle = stringResource(R.string.about_rate_desc), onClick = { context.openStoreListing() })
                if (BuildConfig.SUPPORT_EMAIL.isNotBlank()) {
                    NavRow(
                        stringResource(R.string.about_contact),
                        subtitle = BuildConfig.SUPPORT_EMAIL,
                        onClick = { context.sendFeedbackEmail(BuildConfig.SUPPORT_EMAIL) },
                    )
                }
                Spacer(Modifier.height(Spacing.xxl))
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        }
    }
}

@Composable
fun DocumentScreen(document: String, onBack: () -> Unit) {
    val (title, raw) = when (document) {
        DocumentRoute.LICENSES -> R.string.about_licenses to R.raw.licenses
        DocumentRoute.PRIVACY -> R.string.about_privacy to R.raw.privacy_policy
        else -> R.string.about_changelog to R.raw.changelog
    }
    val text = rememberRawText(raw)
    Column(Modifier.fillMaxSize()) {
        SukoonTopBar(stringResource(title), onBack = onBack, modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
        when (text) {
            null -> LoadingState()
            "" -> ErrorState(
                Icons.Outlined.ErrorOutline,
                stringResource(R.string.error_generic_title),
                stringResource(R.string.error_generic_body),
                onRetry = onBack,
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val blocks = text.split("\n\n")
                items(blocks.size) { i ->
                    val block = blocks[i]
                    val isHeading = block.startsWith("# ")
                    Text(
                        text = block.removePrefix("# "),
                        style = if (isHeading) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                        fontFamily = if (document == DocumentRoute.LICENSES && !isHeading) FontFamily.Monospace else null,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.gutter, vertical = if (isHeading) Spacing.md else Spacing.xs)
                            .then(if (isHeading) Modifier.semantics { heading() } else Modifier),
                    )
                }
                item { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars).height(Spacing.xxl)) }
            }
        }
    }
}

/** null while loading, "" on failure. Read off the main thread. */
@Composable
private fun rememberRawText(@RawRes id: Int): String? {
    val context = LocalContext.current
    var text by remember(id) { mutableStateOf<String?>(null) }
    LaunchedEffect(id) {
        text = withContext(Dispatchers.IO) {
            runCatching { context.resources.openRawResource(id).bufferedReader().use { it.readText() } }.getOrDefault("")
        }
    }
    return text
}

private fun Context.openUrl(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

private fun Context.openStoreListing() {
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
    try {
        startActivity(market)
    } catch (_: ActivityNotFoundException) {
        openUrl("https://play.google.com/store/apps/details?id=$packageName")
    }
}

private fun Context.sendFeedbackEmail(email: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        putExtra(Intent.EXTRA_SUBJECT, "${getString(R.string.app_name)} ${BuildConfig.VERSION_NAME} feedback")
    }
    runCatching { startActivity(intent) }
}
