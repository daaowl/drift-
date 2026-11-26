package app.sukoon.domain.repository

import app.sukoon.domain.model.CounterProgress
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.DuaCollection
import app.sukoon.domain.model.Section
import app.sukoon.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

interface DuaRepository {
    suspend fun ensureSeeded()
    fun observeSections(): Flow<List<Section>>
    fun observeSection(sectionId: String): Flow<Section?>
    fun observeDuasInSection(sectionId: String): Flow<List<Dua>>
    fun observeDua(duaId: String): Flow<Dua?>
    fun search(query: String): Flow<List<Dua>>
    suspend fun getDua(duaId: String): Dua?
    suspend fun getDuasInSection(sectionId: String): List<Dua>
    suspend fun getAllDuas(): List<Dua>
}

interface UserDataRepository {
    fun observeFavorites(): Flow<List<Dua>>
    suspend fun setFavorite(duaId: String, favorite: Boolean)

    fun observeCollections(): Flow<List<DuaCollection>>
    fun observeCollection(collectionId: Long): Flow<DuaCollection?>
    fun observeCollectionDuas(collectionId: Long): Flow<List<Dua>>
    fun observeCollectionIdsContaining(duaId: String): Flow<Set<Long>>
    suspend fun createCollection(name: String): Long
    suspend fun renameCollection(collectionId: Long, name: String)
    suspend fun deleteCollection(collectionId: Long)
    suspend fun addToCollection(collectionId: Long, duaId: String)
    suspend fun removeFromCollection(collectionId: Long, duaId: String)

    fun observeCounter(duaId: String): Flow<CounterProgress>
    suspend fun saveCounter(progress: CounterProgress)

    suspend fun clearAll()
}

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun update(transform: (UserSettings) -> UserSettings)
    suspend fun resetToDefaults()
}

interface SecurityRepository {
    val hasPin: Flow<Boolean>
    suspend fun setPin(pin: String)
    suspend fun verifyPin(pin: String): Boolean
    suspend fun clearPin()
}

sealed interface ImportResult {
    data class Success(val favorites: Int, val collections: Int) : ImportResult
    data class Failure(val reason: Reason) : ImportResult
    enum class Reason { UNREADABLE, UNSUPPORTED_VERSION }
}

interface BackupRepository {
    suspend fun export(output: OutputStream)
    suspend fun import(input: InputStream): ImportResult
}
