package app.sukoon.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.sukoon.R
import app.sukoon.core.designsystem.component.GeometricOrnament
import app.sukoon.core.designsystem.theme.Radii
import app.sukoon.core.designsystem.theme.Spacing
import app.sukoon.core.designsystem.theme.SukoonTheme
import app.sukoon.domain.repository.SettingsRepository
import app.sukoon.ui.permission.rememberNotificationPermissionRequest
import coil3.compose.AsyncImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    fun setReminders(enabled: Boolean, then: () -> Unit) {
        viewModelScope.launch {
            settings.update { it.copy(dailyEnabled = enabled) }
            then()
        }
    }
}

private data class Page(@DrawableRes val art: Int, @StringRes val title: Int, @StringRes val body: Int)

private val pages = listOf(
    Page(R.drawable.onboarding_browse, R.string.onboarding_1_title, R.string.onboarding_1_body),
    Page(R.drawable.onboarding_read, R.string.onboarding_2_title, R.string.onboarding_2_body),
    Page(R.drawable.onboarding_remind, R.string.onboarding_3_title, R.string.onboarding_3_body),
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val permission = rememberNotificationPermissionRequest(onDenied = { viewModel.setReminders(false, onFinished) })
    val last = pager.currentPage == pages.lastIndex

    Box(Modifier.fillMaxSize()) {
        GeometricOrnament(SukoonTheme.extended.ornament, Modifier.fillMaxSize(), cell = 72.dp)
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Spacing.sm), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { viewModel.setReminders(false, onFinished) },
                    modifier = Modifier.testTag("onboarding_skip"),
                ) { Text(stringResource(R.string.onboarding_skip)) }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
                val page = pages[index]
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    AsyncImage(
                        model = page.art,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .fillMaxWidth(0.7f)
                            .aspectRatio(1f),
                    )
                    Spacer(Modifier.height(Spacing.xxl))
                    Text(
                        stringResource(page.title),
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        stringResource(page.body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 420.dp),
                    )
                }
            }
            Indicator(count = pages.size, current = pager.currentPage)
            Column(
                Modifier.fillMaxWidth().padding(Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = {
                        if (last) permission.request { viewModel.setReminders(true, onFinished) }
                        else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    },
                    shape = RoundedCornerShape(Radii.pill),
                    modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().height(56.dp),
                ) {
                    Text(stringResource(if (last) R.string.onboarding_enable_reminders else R.string.onboarding_next))
                }
                if (last) {
                    TextButton(
                        onClick = { viewModel.setReminders(false, onFinished) },
                        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().height(48.dp),
                    ) { Text(stringResource(R.string.onboarding_later)) }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun Indicator(count: Int, current: Int) {
    val label = stringResource(R.string.onboarding_page, current + 1, count)
    Row(
        Modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = label },
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 24.dp else 8.dp, label = "dot")
            val color by animateColorAsState(
                if (i == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                label = "dotColor",
            )
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = width, height = 8.dp)
                    .clip(RoundedCornerShape(Radii.pill))
                    .background(color),
            )
        }
    }
}
