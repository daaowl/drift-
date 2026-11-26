package app.sukoon

import app.sukoon.data.search.SearchNormalizer
import app.sukoon.domain.model.CounterProgress
import app.sukoon.domain.model.DailySource
import app.sukoon.domain.model.DuaKind
import app.sukoon.domain.model.UserSettings
import app.sukoon.domain.usecase.CounterUseCase
import app.sukoon.domain.usecase.GetDuaOfTheDayUseCase
import app.sukoon.notifications.ReminderScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class SearchNormalizerTest {
    @Test fun spellingVariantsMatch() {
        val stored = SearchNormalizer.normalize("Hasbunallahu wa ni'mal wakeel")
        val query = SearchNormalizer.toMatchQuery("hasbunallah")!!.removeSuffix("*")
        assertTrue(stored.split(' ').any { it.startsWith(query) })
    }

    @Test fun diacriticsAndApostrophesAreStripped() {
        assertEquals("alahuma inia", SearchNormalizer.normalize("Allâhumma innî’a"))
    }

    @Test fun eeAndOoCollapse() {
        assertEquals(SearchNormalizer.normalize("Raheem"), SearchNormalizer.normalize("rahim"))
        assertEquals(SearchNormalizer.normalize("lanakoonanna"), SearchNormalizer.normalize("lanakunana"))
    }

    @Test fun blankQueryHasNoMatchExpression() {
        assertNull(SearchNormalizer.toMatchQuery("  ' - "))
    }

    @Test fun eachTokenGetsPrefixWildcard() {
        assertEquals("rabi* zidni*", SearchNormalizer.toMatchQuery("Rabbi zidni"))
    }
}

class DuaOfTheDayTest {
    private val pool = (1..30).map { dua("d$it") }

    @Test fun sameDaySameDua() {
        val day = LocalDate.of(2026, 11, 26)
        assertEquals(GetDuaOfTheDayUseCase.pick(pool, day), GetDuaOfTheDayUseCase.pick(pool.shuffled(), day))
    }

    @Test fun changesAcrossDays() {
        val picks = (0L until 14L).map { GetDuaOfTheDayUseCase.pick(pool, LocalDate.of(2026, 1, 1).plusDays(it))!!.id }.toSet()
        assertTrue(picks.size > 5)
    }

    @Test fun emptyPoolReturnsNull() {
        assertNull(GetDuaOfTheDayUseCase.pick(emptyList(), LocalDate.now()))
    }

    @Test fun guidanceIsNeverPicked() = runTest {
        val repo = FakeDuaRepository(duas = listOf(dua("g", kind = DuaKind.GUIDANCE), dua("x")))
        repeat(20) { i ->
            val d = GetDuaOfTheDayUseCase(repo)(UserSettings(), LocalDate.of(2026, 1, 1).plusDays(i.toLong()))
            assertEquals("x", d?.id)
        }
    }

    @Test fun respectsChosenSection() = runTest {
        val repo = FakeDuaRepository(duas = listOf(dua("a", section = "love"), dua("b", section = "health")))
        val d = GetDuaOfTheDayUseCase(repo)(UserSettings(dailySource = DailySource.SECTION, dailySectionId = "health"))
        assertEquals("b", d?.id)
    }
}

class CounterUseCaseTest {
    private val duas = FakeDuaRepository()
    private val user = FakeUserDataRepository(duas)
    private val counter = CounterUseCase(user)

    @Test fun completesAndAutoResets() = runTest {
        var p = CounterProgress("a", 0, 0)
        var completed = false
        repeat(7) { val r = counter.increment(p, 7, autoReset = true); p = r.first; completed = r.second }
        assertTrue(completed)
        assertEquals(0, p.count)
        assertEquals(1, p.completedRounds)
        assertEquals(p, user.counters.value["a"])
    }

    @Test fun holdsAtTargetWithoutAutoReset() = runTest {
        var p = CounterProgress("a", 2, 0)
        p = counter.increment(p, 3, autoReset = false).first
        assertEquals(3, p.count)
        assertEquals(1, p.completedRounds)
    }

    @Test fun openEndedNeverCompletes() = runTest {
        var p = CounterProgress("a", 0, 0)
        repeat(50) {
            val r = counter.increment(p, 0, autoReset = true)
            assertFalse(r.second)
            p = r.first
        }
        assertEquals(50, p.count)
    }

    @Test fun resetZeroes() = runTest {
        counter.reset(CounterProgress("a", 5, 2))
        assertEquals(CounterProgress("a", 0, 0), user.counters.value["a"])
    }
}

class ReminderSchedulerTest {
    @Test fun laterTodayWhenTimeAhead() {
        val now = LocalDateTime.of(2026, 11, 26, 8, 0)
        assertEquals(Duration.ofMinutes(60), ReminderScheduler.delayUntil(9 * 60, now))
    }

    @Test fun tomorrowWhenTimePassed() {
        val now = LocalDateTime.of(2026, 11, 26, 20, 0)
        assertEquals(Duration.ofHours(11), ReminderScheduler.delayUntil(7 * 60, now))
    }

    @Test fun planIgnoresDisabled() {
        val plan = ReminderScheduler.Plan.from(UserSettings(dailyEnabled = false, morningEnabled = true))
        assertNull(plan.daily)
        assertNotNull(plan.morning)
        assertNull(plan.evening)
    }
}
