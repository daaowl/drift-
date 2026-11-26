package app.sukoon.domain.usecase

import app.sukoon.domain.model.CounterProgress
import app.sukoon.domain.model.DailySource
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.DuaKind
import app.sukoon.domain.model.ReaderList
import app.sukoon.domain.model.UserSettings
import app.sukoon.domain.repository.DuaRepository
import app.sukoon.domain.repository.UserDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class ObserveSectionsUseCase @Inject constructor(private val repo: DuaRepository) {
    operator fun invoke() = repo.observeSections()
}

class ObserveSectionUseCase @Inject constructor(private val repo: DuaRepository) {
    operator fun invoke(sectionId: String) = repo.observeSection(sectionId)
}

class ObserveSectionDuasUseCase @Inject constructor(private val repo: DuaRepository) {
    operator fun invoke(sectionId: String) = repo.observeDuasInSection(sectionId)
}

class ObserveReaderListUseCase @Inject constructor(
    private val duas: DuaRepository,
    private val user: UserDataRepository,
) {
    operator fun invoke(list: ReaderList): Flow<List<Dua>> = when (list) {
        is ReaderList.SectionList -> duas.observeDuasInSection(list.sectionId)
        ReaderList.Favorites -> user.observeFavorites()
        is ReaderList.Collection -> user.observeCollectionDuas(list.collectionId)
    }
}

class SearchDuasUseCase @Inject constructor(private val repo: DuaRepository) {
    operator fun invoke(queries: Flow<String>): Flow<List<Dua>?> = queries
        .map { it.trim() }
        .distinctUntilChanged()
        .debounce { if (it.isEmpty()) 0L else 180L }
        .flatMapLatest { q ->
            if (q.isBlank()) flowOf(null) else repo.search(q)
        }
}

class ToggleFavoriteUseCase @Inject constructor(private val repo: UserDataRepository) {
    suspend operator fun invoke(dua: Dua) = repo.setFavorite(dua.id, !dua.isFavorite)
}

/**
 * Picks a stable dua for a given day: the same dua all day, a different one tomorrow.
 * Guidance entries (non-recitable notes) are never picked.
 */
class GetDuaOfTheDayUseCase @Inject constructor(private val repo: DuaRepository) {
    suspend operator fun invoke(settings: UserSettings, date: LocalDate = LocalDate.now()): Dua? {
        val pool = when {
            settings.dailySource == DailySource.SECTION && settings.dailySectionId != null ->
                repo.getDuasInSection(settings.dailySectionId)
            else -> repo.getAllDuas()
        }.filter { it.kind == DuaKind.DUA }
        return pick(pool, date)
    }

    companion object {
        fun pick(pool: List<Dua>, date: LocalDate): Dua? {
            if (pool.isEmpty()) return null
            val sorted = pool.sortedBy { it.id }
            val seed = date.toEpochDay() * 2654435761L
            val index = Math.floorMod(seed xor (seed ushr 16), sorted.size.toLong()).toInt()
            return sorted[index]
        }
    }
}

class CounterUseCase @Inject constructor(private val repo: UserDataRepository) {
    fun observe(duaId: String) = repo.observeCounter(duaId)

    /** Returns the new progress and whether this tap completed a round. */
    suspend fun increment(current: CounterProgress, target: Int, autoReset: Boolean): Pair<CounterProgress, Boolean> {
        val next = current.count + 1
        val completed = target > 0 && next >= target
        val progress = when {
            completed && autoReset -> current.copy(count = 0, completedRounds = current.completedRounds + 1)
            completed -> current.copy(count = target, completedRounds = current.completedRounds + 1)
            else -> current.copy(count = next)
        }
        repo.saveCounter(progress)
        return progress to completed
    }

    suspend fun reset(current: CounterProgress) {
        repo.saveCounter(current.copy(count = 0, completedRounds = 0))
    }
}

class ObserveDuaWithCollectionsUseCase @Inject constructor(
    private val duas: DuaRepository,
    private val user: UserDataRepository,
) {
    operator fun invoke(duaId: String) =
        combine(duas.observeDua(duaId), user.observeCollectionIdsContaining(duaId)) { d, ids -> d to ids }
}
