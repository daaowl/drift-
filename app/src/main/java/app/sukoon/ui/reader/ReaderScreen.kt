package app.sukoon.ui.reader

import android.content.ClipData
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sukoon.R
import app.sukoon.core.designsystem.component.EmptyState
import app.sukoon.core.designsystem.component.ErrorState
import app.sukoon.core.designsystem.component.FavoriteButton
import app.sukoon.core.designsystem.component.LoadingState
import app.sukoon.core.designsystem.component.SukoonTopBar
import app.sukoon.core.feedback.rememberHaptics
import app.sukoon.core.designsystem.theme.Spacing
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.DuaKind
import app.sukoon.domain.model.LoadState
import app.sukoon.domain.model.ReaderList
import app.sukoon.domain.model.UserSettings
import app.sukoon.ui.main.LocalAppEnvironment
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onOpenCounter: (String) -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val inCollections by viewModel.collectionsOfCurrent.collectAsStateWithLifecycle()
    val settings = LocalAppEnvironment.current.settings

    KeepScreenOn(settings.keepScreenOn)

    Column(Modifier.fillMaxSize()) {
        when (val s = state) {
            LoadState.Loading -> {
                SukoonTopBar(title = "", onBack = onBack, modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
                LoadingState()
            }
            is LoadState.Error -> {
                SukoonTopBar(title = "", onBack = onBack, modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
                ErrorState(
                    icon = Icons.Outlined.ErrorOutline,
                    title = stringResource(R.string.error_dua_title),
                    body = stringResource(R.string.error_dua_body),
                    onRetry = viewModel::retry,
                )
            }
            is LoadState.Success -> {
                val content = s.value
                if (content.duas.isEmpty()) {
                    SukoonTopBar(title = "", onBack = onBack, modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
                    EmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = stringResource(R.string.reader_empty_title),
                        body = stringResource(R.string.reader_empty_body),
                        actionLabel = stringResource(R.string.action_go_back),
                        onAction = onBack,
                    )
                } else {
                    ReaderPager(
                        content = content,
                        settings = settings,
                        collections = collections.map { it.id to it.name },
                        inCollections = inCollections,
                        onBack = onBack,
                        onOpenCounter = onOpenCounter,
                        onPageSettled = viewModel::onPageSettled,
                        onFirstShown = viewModel::onFirstShown,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onToggleCollection = viewModel::setInCollection,
                        onCreateCollection = viewModel::createCollectionWith,
                        onPin = viewModel::pinToWidget,
                        onShowArabic = viewModel::setShowArabic,
                        onShowTransliteration = viewModel::setShowTransliteration,
                        onShowTranslation = viewModel::setShowTranslation,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderPager(
    content: ReaderContent,
    settings: UserSettings,
    collections: List<Pair<Long, String>>,
    inCollections: Set<Long>,
    onBack: () -> Unit,
    onOpenCounter: (String) -> Unit,
    onPageSettled: (Dua) -> Unit,
    onFirstShown: (Dua) -> Unit,
    onToggleFavorite: (Dua) -> Unit,
    onToggleCollection: (Long, String, Boolean) -> Unit,
    onCreateCollection: (String, String) -> Unit,
    onPin: (String) -> Unit,
    onShowArabic: (Boolean) -> Unit,
    onShowTransliteration: (Boolean) -> Unit,
    onShowTranslation: (Boolean) -> Unit,
) {
    val duas = content.duas
    val pager = rememberPagerState(initialPage = content.initialIndex.coerceIn(0, duas.lastIndex)) { duas.size }
    val haptics = rememberHaptics()
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val copiedMessage = stringResource(R.string.reader_copied)
    val pinnedMessage = stringResource(R.string.reader_pinned)
    val shareTitle = stringResource(R.string.reader_share_title)

    var showDisplayMenu by rememberSaveable { mutableStateOf(false) }
    var showMoreMenu by rememberSaveable { mutableStateOf(false) }
    var showCollectionSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { duas.getOrNull(pager.currentPage)?.let(onFirstShown) }
    LaunchedEffect(pager, duas) {
        snapshotFlow { pager.settledPage }.distinctUntilChanged().collect { page ->
            duas.getOrNull(page)?.let(onPageSettled)
        }
    }
    LaunchedEffect(pager) {
        snapshotFlow { pager.currentPage }.distinctUntilChanged().collect { haptics.tick(app.sukoon.domain.model.HapticStrength.LIGHT) }
    }

    val current = duas[pager.currentPage.coerceIn(0, duas.lastIndex)]
    val listTitle = when (content.list) {
        is ReaderList.SectionList -> content.sectionTitle.orEmpty()
        ReaderList.Favorites -> stringResource(R.string.saved_favorites)
        is ReaderList.Collection -> stringResource(R.string.saved_collection)
    }

    Column(Modifier.fillMaxSize()) {
        SukoonTopBar(
            title = listTitle,
            onBack = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        ) {
            IconButton(onClick = { showDisplayMenu = true }) {
                Icon(Icons.Outlined.FormatSize, contentDescription = stringResource(R.string.cd_display_options))
            }
            DropdownMenu(expanded = showDisplayMenu, onDismissRequest = { showDisplayMenu = false }) {
                ToggleItem(stringResource(R.string.reader_show_arabic), settings.showArabic, onShowArabic)
                ToggleItem(stringResource(R.string.reader_show_transliteration), settings.showTransliteration, onShowTransliteration)
                ToggleItem(stringResource(R.string.reader_show_translation), settings.showTranslation, onShowTranslation)
            }
            IconButton(onClick = { showMoreMenu = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.cd_more))
            }
            DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_copy)) },
                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                    onClick = {
                        showMoreMenu = false
                        clipboard.setText(AnnotatedString(current.shareText()))
                        scope.launch { env.snackbar.showSnackbar(copiedMessage) }
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_share)) },
                    leadingIcon = { Icon(Icons.Outlined.Share, null) },
                    onClick = {
                        showMoreMenu = false
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, current.shareText())
                            clipData = ClipData.newPlainText(current.title, current.shareText())
                        }
                        context.startActivity(Intent.createChooser(send, shareTitle))
                    },
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_add_to_collection)) },
                    leadingIcon = { Icon(Icons.Outlined.LibraryAdd, null) },
                    onClick = {
                        showMoreMenu = false
                        showCollectionSheet = true
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reader_pin_widget)) },
                    leadingIcon = { Icon(Icons.Outlined.PushPin, null) },
                    onClick = {
                        showMoreMenu = false
                        onPin(current.id)
                        scope.launch { env.snackbar.showSnackbar(pinnedMessage) }
                    },
                )
            }
        }

        HorizontalPager(
            state = pager,
            key = { duas[it].id },
            beyondViewportPageCount = 1,
            modifier = Modifier
                .weight(1f)
                .testTag("reader_pager"),
        ) { page ->
            DuaPage(
                dua = duas[page],
                settings = settings,
                sectionTitle = if (content.list is ReaderList.SectionList) null else content.sectionTitle,
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = Spacing.gutter, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            FavoriteButton(current.isFavorite, onToggle = {
                haptics.confirm()
                onToggleFavorite(current)
            })
            IconButton(onClick = { showCollectionSheet = true }) {
                Icon(Icons.Outlined.LibraryAdd, contentDescription = stringResource(R.string.reader_add_to_collection))
            }
            AnimatedContent(
                targetState = pager.currentPage + 1,
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(120)) },
                label = "pageIndicator",
                modifier = Modifier
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) { n ->
                Text(
                    stringResource(R.string.reader_position, n, duas.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (current.kind == DuaKind.DUA) {
                ExtendedFloatingActionButton(
                    onClick = { onOpenCounter(current.id) },
                    icon = { Icon(Icons.Outlined.TouchApp, contentDescription = null) },
                    text = {
                        Text(
                            if (current.repeatCount != null) stringResource(R.string.reader_count_target, current.repeatCount)
                            else stringResource(R.string.reader_count),
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }

    if (showCollectionSheet) {
        CollectionSheet(
            collections = collections,
            selected = inCollections,
            onToggle = { id, include -> onToggleCollection(id, current.id, include) },
            onCreate = { name -> onCreateCollection(name, current.id) },
            onDismiss = { showCollectionSheet = false },
        )
    }
}

@Composable
private fun ToggleItem(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        trailingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
        onClick = { onChange(!checked) },
    )
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

fun Dua.shareText(): String = buildString {
    appendLine(title)
    if (arabic.isNotBlank()) { appendLine(); appendLine(arabic) }
    if (transliteration.isNotBlank()) { appendLine(); appendLine(transliteration) }
    appendLine(); appendLine(translation)
    appendLine(); append("— ").append(reference)
}
