package app.sukoon

import app.sukoon.domain.model.CounterProgress
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.DuaCollection
import app.sukoon.domain.model.DuaKind
import app.sukoon.domain.model.Section
import app.sukoon.domain.model.UserSettings
import app.sukoon.domain.repository.DuaRepository
import app.sukoon.domain.repository.SettingsRepository
import app.sukoon.domain.repository.UserDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class MainDispatcherRule(val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

fun dua(id: String, section: String = "tranquility", title: String = id, repeat: Int? = null, kind: DuaKind = DuaKind.DUA) = Dua(
    id = id, sectionId = section, title = title, arabic = "رَبِّ", transliteration = "Rabbi $title",
    translation = "My Lord $title", reference = "Quran 1:1", recitationNote = null, repeatCount = repeat,
    order = 1, kind = kind,
)

class FakeDuaRepository(
    val sections: List<Section> = listOf(Section("tranquility", "Tranquility", "Hope", 1, 2)),
    duas: List<Dua> = listOf(dua("a"), dua("b")),
) : DuaRepository {
    val all = MutableStateFlow(duas)
    val favorites = MutableStateFlow<Set<String>>(emptySet())
    var seeded = false

    private fun withFav(list: List<Dua>, fav: Set<String>) = list.map { it.copy(isFavorite = it.id in fav) }

    override suspend fun ensureSeeded() { seeded = true }
    override fun observeSections(): Flow<List<Section>> = MutableStateFlow(sections)
    override fun observeSection(sectionId: String): Flow<Section?> = MutableStateFlow(sections.firstOrNull { it.id == sectionId })
    override fun observeDuasInSection(sectionId: String): Flow<List<Dua>> =
        combine(all, favorites) { l, f -> withFav(l.filter { it.sectionId == sectionId }, f) }
    override fun observeDua(duaId: String): Flow<Dua?> = combine(all, favorites) { l, f -> withFav(l, f).firstOrNull { it.id == duaId } }
    override fun search(query: String): Flow<List<Dua>> =
        all.map { l -> l.filter { it.title.contains(query, ignoreCase = true) || it.transliteration.contains(query, ignoreCase = true) } }
    override suspend fun getDua(duaId: String): Dua? = all.value.firstOrNull { it.id == duaId }
    override suspend fun getDuasInSection(sectionId: String): List<Dua> = all.value.filter { it.sectionId == sectionId }
    override suspend fun getAllDuas(): List<Dua> = all.value
}

class FakeUserDataRepository(private val duas: FakeDuaRepository) : UserDataRepository {
    val counters = MutableStateFlow<Map<String, CounterProgress>>(emptyMap())
    private val collections = MutableStateFlow<List<DuaCollection>>(emptyList())
    private val items = MutableStateFlow<Map<Long, List<String>>>(emptyMap())

    override fun observeFavorites(): Flow<List<Dua>> =
        combine(duas.all, duas.favorites) { l, f -> l.filter { it.id in f }.map { it.copy(isFavorite = true) } }
    override suspend fun setFavorite(duaId: String, favorite: Boolean) {
        duas.favorites.value = if (favorite) duas.favorites.value + duaId else duas.favorites.value - duaId
    }
    override fun observeCollections(): Flow<List<DuaCollection>> = collections
    override fun observeCollection(collectionId: Long): Flow<DuaCollection?> = collections.map { l -> l.firstOrNull { it.id == collectionId } }
    override fun observeCollectionDuas(collectionId: Long): Flow<List<Dua>> =
        combine(duas.all, items) { l, i -> l.filter { it.id in i[collectionId].orEmpty() } }
    override fun observeCollectionIdsContaining(duaId: String): Flow<Set<Long>> =
        items.map { m -> m.filterValues { duaId in it }.keys }
    override suspend fun createCollection(name: String): Long {
        val id = (collections.value.maxOfOrNull { it.id } ?: 0) + 1
        collections.value = collections.value + DuaCollection(id, name, 0, id)
        return id
    }
    override suspend fun renameCollection(collectionId: Long, name: String) {
        collections.value = collections.value.map { if (it.id == collectionId) it.copy(name = name) else it }
    }
    override suspend fun deleteCollection(collectionId: Long) {
        collections.value = collections.value.filterNot { it.id == collectionId }
    }
    override suspend fun addToCollection(collectionId: Long, duaId: String) {
        items.value = items.value + (collectionId to (items.value[collectionId].orEmpty() + duaId).distinct())
    }
    override suspend fun removeFromCollection(collectionId: Long, duaId: String) {
        items.value = items.value + (collectionId to items.value[collectionId].orEmpty() - duaId)
    }
    override fun observeCounter(duaId: String): Flow<CounterProgress> =
        counters.map { it[duaId] ?: CounterProgress(duaId, 0, 0) }
    override suspend fun saveCounter(progress: CounterProgress) {
        counters.value = counters.value + (progress.duaId to progress)
    }
    override suspend fun clearAll() {
        duas.favorites.value = emptySet()
        collections.value = emptyList()
        counters.value = emptyMap()
    }
}

class FakeSettingsRepository(initial: UserSettings = UserSettings()) : SettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: Flow<UserSettings> = state
    override suspend fun update(transform: (UserSettings) -> UserSettings) { state.value = transform(state.value) }
    override suspend fun resetToDefaults() { state.value = UserSettings(onboardingDone = state.value.onboardingDone) }
}
