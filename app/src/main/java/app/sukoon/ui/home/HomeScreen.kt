package app.sukoon.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sukoon.R
import app.sukoon.core.designsystem.component.ArabicText
import app.sukoon.core.designsystem.component.ErrorState
import app.sukoon.core.designsystem.component.Eyebrow
import app.sukoon.core.designsystem.component.FavoriteButton
import app.sukoon.core.designsystem.component.GeometricOrnament
import app.sukoon.core.designsystem.component.SectionCard
import app.sukoon.core.designsystem.component.SkeletonList
import app.sukoon.core.designsystem.component.sharedBoundsFor
import app.sukoon.core.designsystem.theme.Radii
import app.sukoon.core.designsystem.theme.Spacing
import app.sukoon.core.designsystem.theme.SukoonTheme
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.LoadState
import java.time.LocalTime

@Composable
fun HomeScreen(
    onOpenSection: (String) -> Unit,
    onOpenDua: (String) -> Unit,
    onSearch: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreenContent(
        state = state,
        onOpenSection = onOpenSection,
        onOpenDua = onOpenDua,
        onSearch = onSearch,
        onToggleFavorite = viewModel::toggleFavorite,
        onRetry = viewModel::retry,
    )
}

@Composable
fun HomeScreenContent(
    state: LoadState<HomeContent>,
    onOpenSection: (String) -> Unit,
    onOpenDua: (String) -> Unit,
    onSearch: () -> Unit,
    onToggleFavorite: (Dua) -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        LoadState.Loading -> Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) { SkeletonList() }
        is LoadState.Error -> ErrorState(
            icon = Icons.Outlined.ErrorOutline,
            title = stringResource(R.string.error_generic_title),
            body = stringResource(R.string.error_generic_body),
            onRetry = onRetry,
        )
        is LoadState.Success -> HomeGrid(state.value, onOpenSection, onOpenDua, onSearch, onToggleFavorite)
    }
}

@Composable
private fun HomeGrid(
    content: HomeContent,
    onOpenSection: (String) -> Unit,
    onOpenDua: (String) -> Unit,
    onSearch: () -> Unit,
    onToggleFavorite: (Dua) -> Unit,
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 164.dp),
        contentPadding = PaddingValues(start = Spacing.gutter, end = Spacing.gutter, bottom = Spacing.xxl),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_sections"),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            Header(onSearch)
        }
        content.duaOfTheDay?.let { dua ->
            item(key = "daily", span = { GridItemSpan(maxLineSpan) }) {
                AnimatedVisibility(
                    visible = appeared,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 8 },
                ) {
                    DailyCard(dua, onOpen = { onOpenDua(dua.id) }, onToggleFavorite = { onToggleFavorite(dua) })
                }
            }
        }
        content.continueReading?.let { dua ->
            item(key = "continue", span = { GridItemSpan(maxLineSpan) }) {
                ContinueCard(dua) { onOpenDua(dua.id) }
            }
        }
        item(key = "sections-title", span = { GridItemSpan(maxLineSpan) }) {
            Text(
                stringResource(R.string.home_browse_by_need),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(top = Spacing.lg)
                    .semantics { heading() },
            )
        }
        items(content.sections, key = { it.id }) { section ->
            SectionCard(section = section, onClick = { onOpenSection(section.id) })
        }
    }
}

@Composable
private fun Header(onSearch: () -> Unit) {
    val greeting = when (LocalTime.now().hour) {
        in 4..11 -> R.string.greeting_morning
        in 12..16 -> R.string.greeting_afternoon
        else -> R.string.greeting_evening
    }
    Column(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = Spacing.xl, bottom = Spacing.sm),
    ) {
        Eyebrow(stringResource(R.string.salaam))
        Spacer(Modifier.height(Spacing.xs))
        Text(
            stringResource(greeting),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.lg))
        Surface(
            onClick = onSearch,
            shape = RoundedCornerShape(Radii.pill),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .sharedBoundsFor("search-field"),
        ) {
            Row(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(Spacing.md))
                Text(stringResource(R.string.search_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DailyCard(dua: Dua, onOpen: () -> Unit, onToggleFavorite: () -> Unit) {
    Surface(
        onClick = onOpen,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .sharedBoundsFor("dua-${dua.id}"),
    ) {
        Box {
            GeometricOrnament(SukoonTheme.extended.ornament, Modifier.matchParentSize(), cell = 64.dp)
            Column(Modifier.padding(Spacing.xl)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow(stringResource(R.string.home_dua_of_the_day), Modifier.weight(1f))
                    FavoriteButton(dua.isFavorite, onToggleFavorite)
                }
                Spacer(Modifier.height(Spacing.sm))
                ArabicText(dua.arabic, maxLines = 4)
                Spacer(Modifier.height(Spacing.md))
                Text(
                    dua.translation,
                    style = SukoonTheme.reading.translation.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Spacing.md))
                Text(
                    "${dua.title} · ${dua.reference}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ContinueCard(dua: Dua, onOpen: () -> Unit) {
    Surface(
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(Radii.md)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.size(Spacing.lg))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_continue), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Text(dua.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
