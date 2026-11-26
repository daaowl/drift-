package app.sukoon.ui.saved

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sukoon.R
import app.sukoon.core.designsystem.component.DuaRow
import app.sukoon.core.designsystem.component.EmptyState
import app.sukoon.core.designsystem.component.ErrorState
import app.sukoon.core.designsystem.component.SkeletonList
import app.sukoon.core.designsystem.theme.Spacing
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.DuaCollection
import app.sukoon.domain.model.LoadState

@Composable
fun SavedScreen(
    onOpenFavorite: (String) -> Unit,
    onOpenCollection: (Long) -> Unit,
    onBrowse: () -> Unit,
    viewModel: SavedViewModel = hiltViewModel(),
) {
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            Text(
                stringResource(R.string.nav_saved),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier
                    .padding(horizontal = Spacing.gutter, vertical = Spacing.xl)
                    .semantics { heading() },
            )
            SecondaryTabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                Tab(selected = tab == 0, onClick = { viewModel.selectTab(0) }, text = { Text(stringResource(R.string.saved_favorites)) })
                Tab(selected = tab == 1, onClick = { viewModel.selectTab(1) }, text = { Text(stringResource(R.string.saved_collections)) })
            }
            AnimatedContent(targetState = tab, label = "savedTab", modifier = Modifier.weight(1f)) { t ->
                if (t == 0) {
                    FavoritesList(favorites, onOpenFavorite, viewModel::toggleFavorite, onBrowse, viewModel::retry)
                } else {
                    CollectionsList(collections, onOpenCollection, onCreate = { creating = true }, onRetry = viewModel::retry)
                }
            }
        }
        if (tab == 1 && (collections as? LoadState.Success)?.value?.isNotEmpty() == true) {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text(stringResource(R.string.collections_new)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.xl),
            )
        }
    }

    if (creating) {
        NameDialog(
            title = stringResource(R.string.collections_new),
            confirmLabel = stringResource(R.string.collections_create),
            onConfirm = {
                viewModel.createCollection(it)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
}

@Composable
fun FavoritesList(
    state: LoadState<List<Dua>>,
    onOpen: (String) -> Unit,
    onToggleFavorite: (Dua) -> Unit,
    onBrowse: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        LoadState.Loading -> SkeletonList()
        is LoadState.Error -> ErrorState(
            Icons.Outlined.ErrorOutline,
            stringResource(R.string.error_generic_title),
            stringResource(R.string.error_generic_body),
            onRetry,
        )
        is LoadState.Success -> if (state.value.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.FavoriteBorder,
                title = stringResource(R.string.favorites_empty_title),
                body = stringResource(R.string.favorites_empty_body),
                actionLabel = stringResource(R.string.action_browse),
                onAction = onBrowse,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(Spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(state.value, key = { it.id }) { dua ->
                    DuaRow(dua, onClick = { onOpen(dua.id) }, onToggleFavorite = { onToggleFavorite(dua) }, modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun CollectionsList(
    state: LoadState<List<DuaCollection>>,
    onOpen: (Long) -> Unit,
    onCreate: () -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        LoadState.Loading -> SkeletonList()
        is LoadState.Error -> ErrorState(
            Icons.Outlined.ErrorOutline,
            stringResource(R.string.error_generic_title),
            stringResource(R.string.error_generic_body),
            onRetry,
        )
        is LoadState.Success -> if (state.value.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.CollectionsBookmark,
                title = stringResource(R.string.collections_empty_title),
                body = stringResource(R.string.collections_empty_body),
                actionLabel = stringResource(R.string.collections_new),
                onAction = onCreate,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.gutter, bottom = Spacing.huge * 2),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(state.value, key = { it.id }) { c ->
                    Surface(
                        onClick = { onOpen(c.id) },
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth().animateItem(),
                    ) {
                        Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.size(Spacing.lg))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    pluralStringResource(R.plurals.dua_count, c.itemCount, c.itemCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
