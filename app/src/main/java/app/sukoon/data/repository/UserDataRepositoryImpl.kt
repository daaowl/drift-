package app.sukoon.data.repository

import androidx.room.withTransaction
import app.sukoon.data.local.CollectionEntity
import app.sukoon.data.local.CollectionItemEntity
import app.sukoon.data.local.CounterEntity
import app.sukoon.data.local.FavoriteEntity
import app.sukoon.data.local.SukoonDatabase
import app.sukoon.data.local.toDomain
import app.sukoon.di.IoDispatcher
import app.sukoon.domain.model.CounterProgress
import app.sukoon.domain.model.Dua
import app.sukoon.domain.model.DuaCollection
import app.sukoon.domain.repository.UserDataRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserDataRepositoryImpl @Inject constructor(
    private val db: SukoonDatabase,
    @IoDispatcher private val io: CoroutineDispatcher,
) : UserDataRepository {
    private val dao = db.userDao()

    override fun observeFavorites(): Flow<List<Dua>> =
        dao.observeFavorites().map { l -> l.map { it.toDomain() } }.flowOn(io)

    override suspend fun setFavorite(duaId: String, favorite: Boolean) = withContext(io) {
        if (favorite) dao.insertFavorite(FavoriteEntity(duaId, System.currentTimeMillis()))
        else dao.deleteFavorite(duaId)
    }

    override fun observeCollections(): Flow<List<DuaCollection>> =
        dao.observeCollections().map { l -> l.map { it.toDomain() } }.flowOn(io)

    override fun observeCollection(collectionId: Long): Flow<DuaCollection?> =
        dao.observeCollection(collectionId).map { it?.toDomain() }.flowOn(io)

    override fun observeCollectionDuas(collectionId: Long): Flow<List<Dua>> =
        dao.observeCollectionDuas(collectionId).map { l -> l.map { it.toDomain() } }.flowOn(io)

    override fun observeCollectionIdsContaining(duaId: String): Flow<Set<Long>> =
        dao.observeCollectionIdsContaining(duaId).map { it.toSet() }.flowOn(io)

    override suspend fun createCollection(name: String): Long = withContext(io) {
        dao.insertCollection(CollectionEntity(name = name.trim().take(MAX_NAME), createdAt = System.currentTimeMillis()))
    }

    override suspend fun renameCollection(collectionId: Long, name: String) = withContext(io) {
        dao.renameCollection(collectionId, name.trim().take(MAX_NAME))
    }

    override suspend fun deleteCollection(collectionId: Long) = withContext(io) { dao.deleteCollection(collectionId) }

    override suspend fun addToCollection(collectionId: Long, duaId: String) = withContext(io) {
        dao.insertCollectionItem(CollectionItemEntity(collectionId, duaId, System.currentTimeMillis()))
    }

    override suspend fun removeFromCollection(collectionId: Long, duaId: String) = withContext(io) {
        dao.deleteCollectionItem(collectionId, duaId)
    }

    override fun observeCounter(duaId: String): Flow<CounterProgress> =
        dao.observeCounter(duaId).map { it.toDomain(duaId) }.flowOn(io)

    override suspend fun saveCounter(progress: CounterProgress) = withContext(io) {
        dao.upsertCounter(
            CounterEntity(progress.duaId, progress.count, progress.completedRounds, System.currentTimeMillis()),
        )
    }

    override suspend fun clearAll() = withContext(io) {
        db.withTransaction {
            dao.clearFavorites()
            dao.clearCollections()
            dao.clearCounters()
        }
    }

    companion object {
        const val MAX_NAME = 40
    }
}
