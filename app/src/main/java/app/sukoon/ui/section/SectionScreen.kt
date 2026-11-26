package app.sukoon.ui.section

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.sukoon.R
import app.sukoon.core.designsystem.component.DuaRow
import app.sukoon.core.designsystem.component.EmptyState
import app.sukoon.core.designsystem.component.ErrorState
import app.sukoon.core.designsystem.component.GeometricOrnament
import app.sukoon.core.designsystem.component.SectionIcons
import app.sukoon.core.designsystem.component.SkeletonList
import app.sukoon.core.designsystem.component.SukoonTopBar
import app.sukoon.core.designsystem.component.sharedBoundsFor
import app.sukoon.core.designsystem.theme.SectionTints
import app.sukoon.core.designsystem.theme.Spacing
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.DuaKind
import app.sukoon.domain.model.LoadState
import app.sukoon.domain.model.Section
import app.sukoon.ui.main.LocalAppEnvironment
import app.sukoon.ui.reader.DuaPage

@Composable
fun SectionScreen(
    onBack: () -> Unit,
    onOpenDua: (duaId: String, sectionId: String) -> Unit,
    onOpenCounter: (String) -> Unit,
    viewModel: SectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val env = LocalAppEnvironment.current
    val twoPane = env.widthClass == WindowWidthSizeClass.Expanded

    when (val s = state) {
        LoadState.Loading -> Column(Modifier.fillMaxSize()) {
            SukoonTopBar("", onBack = onBack, modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
            SkeletonList()
        }
        is LoadState.Error -> Column(Modifier.fillMaxSize()) {
            SukoonTopBar("", onBack = onBack, modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
            ErrorState(
                icon = Icons.Outlined.ErrorOutline,
                title = stringResource(R.string.error_generic_title),
                body = stringResource(R.string.error_generic_body),
                onRetry = viewModel::retry,
            )
        }
        is LoadState.Success -> {
            val content = s.value
            if (twoPane) {
                val current = content.duas.firstOrNull { it.id == selected } ?: content.duas.firstOrNull()
                Row(Modifier.fillMaxSize()) {
                    SectionList(
                        content = content,
                        selectedId = current?.id,
                        onBack = onBack,
                        onOpen = { viewModel.select(it.id) },
                        onToggleFavorite = viewModel::toggleFavorite,
                        modifier = Modifier.width(400.dp).fillMaxHeight(),
                    )
                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        if (current != null) {
                            DuaPage(
                                dua = current,
                                settings = env.settings,
                                shareTitle = false,
                                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                            )
                            if (current.kind == DuaKind.DUA) {
                                ExtendedFloatingActionButton(
                                    onClick = { onOpenCounter(current.id) },
                                    icon = { Icon(Icons.Outlined.TouchApp, null) },
                                    text = { Text(stringResource(R.string.reader_count)) },
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .padding(Spacing.xl),
                                )
                            }
                        }
                    }
                }
            } else {
                SectionList(
                    content = content,
                    selectedId = null,
                    onBack = onBack,
                    onOpen = { onOpenDua(it.id, content.section.id) },
                    onToggleFavorite = viewModel::toggleFavorite,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun SectionList(
    content: SectionContent,
    selectedId: String?,
    onBack: () -> Unit,
    onOpen: (Dua) -> Unit,
    onToggleFavorite: (Dua) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    Column(modifier.nestedScroll(scroll.nestedScrollConnection)) {
        SukoonTopBar(
            title = content.section.title,
            onBack = onBack,
            scrollBehavior = scroll,
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        )
        if (content.duas.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Inbox,
                title = stringResource(R.string.section_empty_title),
                body = stringResource(R.string.section_empty_body),
                actionLabel = stringResource(R.string.action_go_back),
                onAction = onBack,
            )
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = Spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier
                .fillMaxSize()
                .testTag("section_duas"),
        ) {
            item(key = "hero") { SectionHero(content.section) }
            items(content.duas, key = { it.id }) { dua ->
                DuaRow(
                    dua = dua,
                    selected = dua.id == selectedId,
                    onClick = { onOpen(dua) },
                    onToggleFavorite = { onToggleFavorite(dua) },
                    modifier = Modifier.animateItem(),
                )
            }
            item(key = "bottom") { Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars).height(Spacing.xl)) }
        }
    }
}

@Composable
private fun SectionHero(section: Section) {
    val tint = SectionTints.of(section.id)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.lg)
            .clip(MaterialTheme.shapes.extraLarge)
            .sharedBoundsFor("section-${section.id}")
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Box(Modifier.matchParentSize().background(tint.copy(alpha = 0.10f)))
        GeometricOrnament(tint.copy(alpha = 0.12f), Modifier.matchParentSize(), cell = 52.dp)
        Column(Modifier.padding(Spacing.xl)) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(SectionIcons.of(section.id), contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(Spacing.lg))
            Text(section.title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(Spacing.xs))
            Text(section.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(Spacing.md))
            Text(
                pluralStringResource(R.plurals.dua_count, section.duaCount, section.duaCount),
                style = MaterialTheme.typography.labelLarge,
                color = tint,
            )
        }
    }
}
