package app.sukoon.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sukoon.R
import app.sukoon.core.designsystem.component.DuaRow
import app.sukoon.core.designsystem.component.EmptyState
import app.sukoon.core.designsystem.component.ErrorState
import app.sukoon.core.designsystem.component.LoadingState
import app.sukoon.core.designsystem.component.SukoonTopBar
import app.sukoon.core.designsystem.theme.Spacing
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.LoadState
import app.sukoon.ui.main.LocalAppEnvironment
import kotlinx.coroutines.launch

@Composable
fun CollectionScreen(
    onBack: () -> Unit,
    onOpenDua: (String) -> Unit,
    onBrowse: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val env = LocalAppEnvironment.current
    val scope = rememberCoroutineScope()
    val removedMsg = stringResource(R.string.collection_removed)
    val undo = stringResource(R.string.action_undo)

    val content = (state as? LoadState.Success)?.value
    // Deleted from elsewhere (or just now): leave the screen.
    LaunchedEffect(state) {
        if (state is LoadState.Success && content == null) onBack()
    }

    Column(Modifier.fillMaxSize()) {
        SukoonTopBar(
            title = content?.collection?.name.orEmpty(),
            onBack = onBack,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        ) {
            if (content != null) {
                IconButton(onClick = { renaming = true }) {
                    Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = stringResource(R.string.collection_rename))
                }
                IconButton(onClick = { deleting = true }) {
                    Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.collection_delete))
                }
            }
        }
        when (val s = state) {
            LoadState.Loading -> LoadingState()
            is LoadState.Error -> ErrorState(
                Icons.Outlined.ErrorOutline,
                stringResource(R.string.error_generic_title),
                stringResource(R.string.error_generic_body),
                viewModel::retry,
            )
            is LoadState.Success -> {
                val c = s.value ?: return@Column
                if (c.duas.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.FolderOpen,
                        title = stringResource(R.string.collection_empty_title),
                        body = stringResource(R.string.collection_empty_body),
                        actionLabel = stringResource(R.string.action_browse),
                        onAction = onBrowse,
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(Spacing.gutter),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        items(c.duas, key = { it.id }) { dua ->
                            SwipeToRemove(
                                onRemove = {
                                    viewModel.remove(dua)
                                    scope.launch {
                                        val r = env.snackbar.showSnackbar(removedMsg, actionLabel = undo, withDismissAction = true)
                                        if (r == SnackbarResult.ActionPerformed) viewModel.undoRemove(dua)
                                    }
                                },
                                modifier = Modifier.animateItem(),
                            ) {
                                DuaRow(dua, onClick = { onOpenDua(dua.id) }, onToggleFavorite = { viewModel.toggleFavorite(dua) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (renaming && content != null) {
        NameDialog(
            title = stringResource(R.string.collection_rename),
            confirmLabel = stringResource(R.string.action_save),
            initial = content.collection.name,
            onConfirm = {
                viewModel.rename(it)
                renaming = false
            },
            onDismiss = { renaming = false },
        )
    }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(stringResource(R.string.collection_delete_title)) },
            text = { Text(stringResource(R.string.collection_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.collection_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun SwipeToRemove(onRemove: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) onRemove()
            value != SwipeToDismissBoxValue.Settled
        },
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.large)
                    .padding(horizontal = Spacing.xl),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Outlined.RemoveCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) { content() }
}

