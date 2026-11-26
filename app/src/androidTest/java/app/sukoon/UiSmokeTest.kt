package app.sukoon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.sukoon.core.designsystem.theme.SukoonTheme
import app.sukoon.domain.model.CounterProgress
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.DuaKind
import app.sukoon.domain.model.LoadState
import app.sukoon.domain.model.Section
import app.sukoon.ui.counter.CounterBody
import app.sukoon.ui.counter.CounterContent
import app.sukoon.ui.home.HomeContent
import app.sukoon.ui.home.HomeScreenContent
import app.sukoon.ui.search.SearchContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UiSmokeTest {
    @get:Rule val compose = createComposeRule()

    private val dua = Dua(
        "solace.a", "solace", "Anxiety & Sorrow", "اللَّهُمَّ إِنِّي عَبْدُكَ", "Allahumma inni 'abduka",
        "O Allah, I am Your servant", "Ahmad 3704", null, 7, 1, DuaKind.DUA,
    )

    @Test fun homeShowsSectionsAndOpensOne() {
        var opened: String? = null
        compose.setContent {
            SukoonTheme {
                HomeScreenContent(
                    state = LoadState.Success(
                        HomeContent(listOf(Section("solace", "Solace", "For grief", 2, 15)), dua, null),
                    ),
                    onOpenSection = { opened = it },
                    onOpenDua = {},
                    onSearch = {},
                    onToggleFavorite = {},
                    onRetry = {},
                )
            }
        }
        compose.onNodeWithText("Solace").assertIsDisplayed().performClick()
        assertEquals("solace", opened)
    }

    @Test fun homeErrorStateOffersRetry() {
        var retried = false
        compose.setContent {
            SukoonTheme {
                HomeScreenContent(LoadState.Error(RuntimeException()), {}, {}, {}, {}, onRetry = { retried = true })
            }
        }
        compose.onNodeWithText("Try again").performClick()
        assertEquals(true, retried)
    }

    @Test fun counterTapReportsIncrement() {
        var taps = 0
        compose.setContent {
            SukoonTheme { CounterBody(CounterContent(dua, CounterProgress(dua.id, 3, 0), 7), hapticsOff = false, onTap = { taps++ }) }
        }
        compose.onNodeWithText("3").assertIsDisplayed()
        compose.onNodeWithTag("counter_tap_area").performClick()
        compose.onNodeWithTag("counter_tap_area").performClick()
        assertEquals(2, taps)
    }

    @Test fun searchShowsNoResultsState() {
        compose.setContent {
            SukoonTheme {
                SearchContent(
                    query = "zzzz",
                    results = LoadState.Success(emptyList()),
                    sectionTitles = emptyMap(),
                    onQueryChange = {},
                    onOpenDua = {},
                    onToggleFavorite = {},
                    autoFocus = false,
                )
            }
        }
        compose.onNodeWithText("No duas found").assertIsDisplayed()
    }

    @Test fun searchResultsOpenDua() {
        var opened: String? = null
        compose.setContent {
            SukoonTheme {
                SearchContent(
                    query = "anx",
                    results = LoadState.Success(listOf(dua)),
                    sectionTitles = mapOf("solace" to "Solace"),
                    onQueryChange = {},
                    onOpenDua = { opened = it },
                    onToggleFavorite = {},
                    autoFocus = false,
                )
            }
        }
        compose.onNodeWithText("Anxiety & Sorrow").performClick()
        assertEquals("solace.a", opened)
    }
}
