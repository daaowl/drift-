package app.sukoon.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sukoon.R
import app.sukoon.core.designsystem.component.DuaRow
import app.sukoon.core.designsystem.component.EmptyState
import app.sukoon.core.designsystem.component.ErrorState
import app.sukoon.core.designsystem.component.Eyebrow
import app.sukoon.core.designsystem.component.sharedBoundsFor
import app.sukoon.core.designsystem.theme.Radii
import app.sukoon.core.designsystem.theme.Spacing
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.LoadState

@Composable
fun SearchScreen(
    onOpenDua: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val titles by viewModel.sectionTitles.collectAsStateWithLifecycle()
    SearchContent(
        query = query,
        results = results,
        sectionTitles = titles,
        onQueryChange = viewModel::onQueryChange,
        onOpenDua = onOpenDua,
        onToggleFavorite = viewModel::toggleFavorite,
    )
}

@Composable
fun SearchContent(
    query: String,
    results: LoadState<List<Dua>?>,
    sectionTitles: Map<String, String>,
    onQueryChange: (String) -> Unit,
    onOpenDua: (String) -> Unit,
    onToggleFavorite: (Dua) -> Unit,
    autoFocus: Boolean = true,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { if (autoFocus && query.isEmpty()) runCatching { focus.requestFocus() } }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding(),
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.cd_clear_search))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(Radii.pill),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter, vertical = Spacing.md)
                .heightIn(min = 56.dp)
                .sharedBoundsFor("search-field")
                .focusRequester(focus)
                .testTag("search_field"),
        )

        when (results) {
            LoadState.Loading -> Unit
            is LoadState.Error -> ErrorState(
                icon = Icons.Outlined.ErrorOutline,
                title = stringResource(R.string.error_generic_title),
                body = stringResource(R.string.error_generic_body),
                onRetry = { onQueryChange(query) },
            )
            is LoadState.Success -> {
                val list = results.value
                when {
                    list == null -> Suggestions(onPick = onQueryChange)
                    list.isEmpty() -> EmptyState(
                        icon = Icons.Outlined.SearchOff,
                        title = stringResource(R.string.search_no_results_title),
                        body = stringResource(R.string.search_no_results_body, query),
                        actionLabel = stringResource(R.string.search_clear),
                        onAction = { onQueryChange("") },
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, bottom = Spacing.xxl),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                        modifier = Modifier.testTag("search_results"),
                    ) {
                        item(key = "count") {
                            Text(
                                androidx.compose.ui.res.pluralStringResource(R.plurals.search_result_count, list.size, list.size),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = Spacing.sm),
                            )
                        }
                        items(list, key = { it.id }) { dua ->
                            DuaRow(
                                dua = dua,
                                showSection = sectionTitles[dua.sectionId],
                                onClick = {
                                    keyboard?.hide()
                                    onOpenDua(dua.id)
                                },
                                onToggleFavorite = { onToggleFavorite(dua) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Suggestions(onPick: (String) -> Unit) {
    val suggestions = stringArrayResource(R.array.search_suggestions)
    Column(Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.lg)) {
        Eyebrow(stringResource(R.string.search_try), Modifier.semantics { heading() })
        Spacer(Modifier.height(Spacing.md))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            suggestions.forEach { s ->
                SuggestionChip(onClick = { onPick(s) }, label = { Text(s) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
    }
}
