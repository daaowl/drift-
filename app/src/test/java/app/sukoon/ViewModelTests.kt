package app.sukoon

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.sukoon.domain.model.LoadState
import app.sukoon.domain.model.UserSettings
import app.sukoon.domain.usecase.GetDuaOfTheDayUseCase
import app.sukoon.domain.usecase.ObserveSectionsUseCase
import app.sukoon.domain.usecase.SearchDuasUseCase
import app.sukoon.domain.usecase.ToggleFavoriteUseCase
import app.sukoon.ui.home.HomeViewModel
import app.sukoon.ui.search.SearchViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val duas = FakeDuaRepository()
    private val user = FakeUserDataRepository(duas)
    private val settings = FakeSettingsRepository(UserSettings(lastReadDuaId = "b"))

    private fun vm() = HomeViewModel(
        ObserveSectionsUseCase(duas), duas, settings, GetDuaOfTheDayUseCase(duas), ToggleFavoriteUseCase(user),
    )

    @Test fun emitsSectionsDailyAndContinue() = runTest {
        vm().state.test {
            var item = awaitItem()
            while (item is LoadState.Loading) item = awaitItem()
            val content = (item as LoadState.Success).value
            assertEquals(1, content.sections.size)
            assertNotNull(content.duaOfTheDay)
            assertEquals("b", content.continueReading?.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun hidesContinueWhenResumeOff() = runTest {
        settings.state.value = UserSettings(lastReadDuaId = "b", resumeLastRead = false)
        vm().state.test {
            var item = awaitItem()
            while (item is LoadState.Loading) item = awaitItem()
            assertNull((item as LoadState.Success).value.continueReading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun toggleFavoriteUpdatesDaily() = runTest {
        val vm = vm()
        vm.state.test {
            var item = awaitItem()
            while (item is LoadState.Loading) item = awaitItem()
            val daily = (item as LoadState.Success).value.duaOfTheDay!!
            vm.toggleFavorite(daily)
            var updated: LoadState.Success<app.sukoon.ui.home.HomeContent>
            do {
                @Suppress("UNCHECKED_CAST")
                updated = awaitItem() as LoadState.Success<app.sukoon.ui.home.HomeContent>
            } while (!updated.value.duaOfTheDay!!.isFavorite)
            assertTrue(updated.value.duaOfTheDay!!.isFavorite)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val duas = FakeDuaRepository(duas = listOf(dua("x", title = "Patience"), dua("y", title = "Healing")))
    private val user = FakeUserDataRepository(duas)

    @Test fun emptyQueryShowsSuggestionsThenResults() = runTest {
        val vm = SearchViewModel(SavedStateHandle(), SearchDuasUseCase(duas), ObserveSectionsUseCase(duas), ToggleFavoriteUseCase(user))
        vm.results.test {
            assertEquals(LoadState.Success(null), awaitItem())
            vm.onQueryChange("heal")
            advanceTimeBy(500)
            val r = awaitItem() as LoadState.Success
            assertEquals(listOf("y"), r.value!!.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun queryRestoredFromSavedState() {
        val vm = SearchViewModel(SavedStateHandle(mapOf("query" to "dua")), SearchDuasUseCase(duas), ObserveSectionsUseCase(duas), ToggleFavoriteUseCase(user))
        assertEquals("dua", vm.query.value)
    }
}
