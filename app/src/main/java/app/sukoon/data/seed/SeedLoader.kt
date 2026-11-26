package app.sukoon.data.seed

import android.content.Context
import androidx.room.withTransaction
import app.sukoon.data.local.DuaEntity
import app.sukoon.data.local.DuaFtsEntity
import app.sukoon.data.local.SectionEntity
import app.sukoon.data.local.SukoonDatabase
import app.sukoon.data.prefs.SettingsDataStore
import app.sukoon.data.search.SearchNormalizer
import app.sukoon.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the bundled dua content into Room. Runs once per content version.
 * Content rows are upserted, never dropped and re-inserted, so favourites, collections
 * and counters that reference them survive a content update.
 */
@Singleton
class SeedLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: SukoonDatabase,
    private val settings: SettingsDataStore,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val mutex = Mutex()

    suspend fun ensureSeeded() = withContext(io) {
        mutex.withLock {
            val seed = readSeed()
            val stored = settings.contentVersion()
            if (stored >= seed.contentVersion && db.contentDao().duaCount() > 0) return@withLock
            apply(seed)
            settings.setContentVersion(seed.contentVersion)
        }
    }

    fun readSeed(): SeedFile = context.assets.open(ASSET).bufferedReader(Charsets.UTF_8).use {
        json.decodeFromString(SeedFile.serializer(), it.readText())
    }

    private suspend fun apply(seed: SeedFile) {
        val dao = db.contentDao()
        db.withTransaction {
            dao.upsertSections(seed.sections.map { SectionEntity(it.id, it.title, it.subtitle, it.order) })
            dao.upsertDuas(
                seed.duas.map {
                    DuaEntity(
                        id = it.id,
                        sectionId = it.sectionId,
                        title = it.title,
                        arabic = it.arabic,
                        transliteration = it.transliteration,
                        translation = it.translation,
                        reference = it.reference,
                        recitationNote = it.recitationNote,
                        repeatCount = it.repeatCount,
                        sortOrder = it.order,
                        kind = it.kind,
                    )
                },
            )
            dao.deleteDuasNotIn(seed.duas.map { it.id })
            dao.deleteSectionsNotIn(seed.sections.map { it.id })
            dao.clearFts()
            val sectionTitles = seed.sections.associate { it.id to it.title }
            dao.insertFts(
                seed.duas.map {
                    DuaFtsEntity(
                        duaId = it.id,
                        body = SearchNormalizer.normalize(
                            listOf(it.title, sectionTitles[it.sectionId].orEmpty(), it.transliteration, it.translation, it.reference)
                                .joinToString(" "),
                        ),
                    )
                },
            )
        }
    }

    companion object {
        const val ASSET = "duas.json"
    }
}
