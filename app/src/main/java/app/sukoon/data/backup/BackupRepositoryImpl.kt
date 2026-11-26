package app.sukoon.data.backup

import androidx.room.withTransaction
import app.sukoon.data.local.CollectionEntity
import app.sukoon.data.local.CollectionItemEntity
import app.sukoon.data.local.CounterEntity
import app.sukoon.data.local.FavoriteEntity
import app.sukoon.data.local.SukoonDatabase
import app.sukoon.data.prefs.SettingsDataStore
import app.sukoon.di.IoDispatcher
import app.sukoon.domain.model.UserSettings
import app.sukoon.domain.repository.BackupRepository
import app.sukoon.domain.repository.ImportResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val createdAt: Long,
    val favorites: List<BackupFavorite> = emptyList(),
    val collections: List<BackupCollection> = emptyList(),
    val counters: List<BackupCounter> = emptyList(),
    val settings: BackupSettings? = null,
) {
    companion object {
        const val FORMAT = "sukoon-backup"
        const val VERSION = 1
    }
}

@Serializable data class BackupFavorite(val duaId: String, val addedAt: Long)
@Serializable data class BackupCollection(val name: String, val createdAt: Long, val duaIds: List<String>)
@Serializable data class BackupCounter(val duaId: String, val count: Int, val completedRounds: Int)

/** Appearance and reading preferences only. Security settings and the PIN are never exported. */
@Serializable
data class BackupSettings(
    val themeMode: String,
    val dynamicColor: Boolean,
    val accent: String,
    val arabicScale: Float,
    val transliterationScale: Float,
    val translationScale: Float,
    val showArabic: Boolean,
    val showTransliteration: Boolean,
    val showTranslation: Boolean,
    val arabicFont: String,
    val counterHaptic: String,
    val counterSound: Boolean,
    val counterAutoReset: Boolean,
)

object BackupCodec {
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    fun decode(text: String): BackupFile? = runCatching { json.decodeFromString(BackupFile.serializer(), text) }
        .getOrNull()
        ?.takeIf { it.format == BackupFile.FORMAT }

    fun UserSettings.toBackup() = BackupSettings(
        themeMode = themeMode.name, dynamicColor = dynamicColor, accent = accent.name,
        arabicScale = arabicScale, transliterationScale = transliterationScale, translationScale = translationScale,
        showArabic = showArabic, showTransliteration = showTransliteration, showTranslation = showTranslation,
        arabicFont = arabicFont.name, counterHaptic = counterHaptic.name, counterSound = counterSound,
        counterAutoReset = counterAutoReset,
    )

    private inline fun <reified E : Enum<E>> String.enumOr(default: E): E =
        enumValues<E>().firstOrNull { it.name == this } ?: default

    fun UserSettings.applyBackup(b: BackupSettings): UserSettings = copy(
        themeMode = b.themeMode.enumOr(themeMode),
        dynamicColor = b.dynamicColor,
        accent = b.accent.enumOr(accent),
        arabicScale = b.arabicScale.coerceIn(UserSettings.MIN_SCALE, UserSettings.MAX_SCALE),
        transliterationScale = b.transliterationScale.coerceIn(UserSettings.MIN_SCALE, UserSettings.MAX_SCALE),
        translationScale = b.translationScale.coerceIn(UserSettings.MIN_SCALE, UserSettings.MAX_SCALE),
        showArabic = b.showArabic,
        showTransliteration = b.showTransliteration,
        showTranslation = b.showTranslation,
        arabicFont = b.arabicFont.enumOr(arabicFont),
        counterHaptic = b.counterHaptic.enumOr(counterHaptic),
        counterSound = b.counterSound,
        counterAutoReset = b.counterAutoReset,
    )
}

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val db: SukoonDatabase,
    private val settings: SettingsDataStore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : BackupRepository {

    override suspend fun export(output: OutputStream) = withContext(io) {
        val dao = db.userDao()
        val file = db.withTransaction {
            val items = dao.allCollectionItems().groupBy { it.collectionId }
            BackupFile(
                createdAt = System.currentTimeMillis(),
                favorites = dao.allFavorites().map { BackupFavorite(it.duaId, it.addedAt) },
                collections = dao.allCollections().map { c ->
                    BackupCollection(c.name, c.createdAt, items[c.id].orEmpty().sortedBy { it.addedAt }.map { it.duaId })
                },
                counters = dao.allCounters().map { BackupCounter(it.duaId, it.count, it.completedRounds) },
                settings = with(BackupCodec) { settings.settings.first().toBackup() },
            )
        }
        // Serialise fully before touching the stream so a failure never leaves a half-written file.
        val bytes = BackupCodec.encode(file).toByteArray(Charsets.UTF_8)
        output.use { it.write(bytes); it.flush() }
    }

    override suspend fun import(input: InputStream): ImportResult = withContext(io) {
        val text = runCatching { input.use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()
            ?: return@withContext ImportResult.Failure(ImportResult.Reason.UNREADABLE)
        val file = BackupCodec.decode(text)
            ?: return@withContext ImportResult.Failure(ImportResult.Reason.UNREADABLE)
        if (file.version > BackupFile.VERSION) {
            return@withContext ImportResult.Failure(ImportResult.Reason.UNSUPPORTED_VERSION)
        }
        val dao = db.userDao()
        var favCount = 0
        db.withTransaction {
            val known = dao.allDuaIds().toSet()
            file.favorites.filter { it.duaId in known }.forEach {
                dao.insertFavorite(FavoriteEntity(it.duaId, it.addedAt))
                favCount++
            }
            file.collections.forEach { c ->
                val name = c.name.trim().take(40).ifBlank { return@forEach }
                val id = dao.insertCollection(CollectionEntity(name = name, createdAt = c.createdAt))
                c.duaIds.filter { it in known }.forEachIndexed { index, duaId ->
                    dao.insertCollectionItem(CollectionItemEntity(id, duaId, c.createdAt + index))
                }
            }
            file.counters.filter { it.duaId in known }.forEach {
                dao.upsertCounter(
                    CounterEntity(it.duaId, it.count.coerceAtLeast(0), it.completedRounds.coerceAtLeast(0), System.currentTimeMillis()),
                )
            }
        }
        file.settings?.let { b -> settings.update { with(BackupCodec) { it.applyBackup(b) } } }
        ImportResult.Success(favorites = favCount, collections = file.collections.size)
    }
}
