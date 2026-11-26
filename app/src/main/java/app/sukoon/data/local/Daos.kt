package app.sukoon.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

private const val DUA_WITH_FAV =
    "SELECT duas.*, (favorites.dua_id IS NOT NULL) AS is_favorite FROM duas " +
        "LEFT JOIN favorites ON favorites.dua_id = duas.id"

@Dao
interface ContentDao {
    @Upsert
    suspend fun upsertSections(sections: List<SectionEntity>)

    @Upsert
    suspend fun upsertDuas(duas: List<DuaEntity>)

    @Query("DELETE FROM duas WHERE id NOT IN (:keepIds)")
    suspend fun deleteDuasNotIn(keepIds: List<String>)

    @Query("DELETE FROM sections WHERE id NOT IN (:keepIds)")
    suspend fun deleteSectionsNotIn(keepIds: List<String>)

    @Query("DELETE FROM dua_fts")
    suspend fun clearFts()

    @Insert
    suspend fun insertFts(rows: List<DuaFtsEntity>)

    @Query("SELECT COUNT(*) FROM duas")
    suspend fun duaCount(): Int
}

@Dao
interface DuaDao {
    @Query(
        "SELECT sections.*, (SELECT COUNT(*) FROM duas WHERE duas.section_id = sections.id) AS dua_count " +
            "FROM sections ORDER BY sort_order",
    )
    fun observeSections(): Flow<List<SectionWithCount>>

    @Query(
        "SELECT sections.*, (SELECT COUNT(*) FROM duas WHERE duas.section_id = sections.id) AS dua_count " +
            "FROM sections WHERE id = :sectionId",
    )
    fun observeSection(sectionId: String): Flow<SectionWithCount?>

    @Query("$DUA_WITH_FAV WHERE duas.section_id = :sectionId ORDER BY duas.sort_order")
    fun observeDuasInSection(sectionId: String): Flow<List<DuaWithFavorite>>

    @Query("$DUA_WITH_FAV WHERE duas.section_id = :sectionId ORDER BY duas.sort_order")
    suspend fun getDuasInSection(sectionId: String): List<DuaWithFavorite>

    @Query("$DUA_WITH_FAV WHERE duas.id = :duaId")
    fun observeDua(duaId: String): Flow<DuaWithFavorite?>

    @Query("$DUA_WITH_FAV WHERE duas.id = :duaId")
    suspend fun getDua(duaId: String): DuaWithFavorite?

    @Query("$DUA_WITH_FAV ORDER BY duas.section_id, duas.sort_order")
    suspend fun getAll(): List<DuaWithFavorite>

    @Query(
        "SELECT duas.*, (favorites.dua_id IS NOT NULL) AS is_favorite FROM duas " +
            "JOIN dua_fts ON dua_fts.dua_id = duas.id " +
            "LEFT JOIN favorites ON favorites.dua_id = duas.id " +
            "WHERE dua_fts MATCH :match ORDER BY duas.title",
    )
    fun search(match: String): Flow<List<DuaWithFavorite>>
}

@Dao
interface UserDao {
    @Query(
        "SELECT duas.*, 1 AS is_favorite FROM favorites JOIN duas ON duas.id = favorites.dua_id " +
            "ORDER BY favorites.added_at DESC",
    )
    fun observeFavorites(): Flow<List<DuaWithFavorite>>

    @Query("SELECT * FROM favorites")
    suspend fun allFavorites(): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFavorite(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE dua_id = :duaId")
    suspend fun deleteFavorite(duaId: String)

    @Query(
        "SELECT collections.*, (SELECT COUNT(*) FROM collection_items WHERE collection_id = collections.id) AS item_count " +
            "FROM collections ORDER BY created_at DESC",
    )
    fun observeCollections(): Flow<List<CollectionWithCount>>

    @Query(
        "SELECT collections.*, (SELECT COUNT(*) FROM collection_items WHERE collection_id = collections.id) AS item_count " +
            "FROM collections WHERE id = :collectionId",
    )
    fun observeCollection(collectionId: Long): Flow<CollectionWithCount?>

    @Query("SELECT * FROM collections ORDER BY created_at")
    suspend fun allCollections(): List<CollectionEntity>

    @Query("SELECT * FROM collection_items")
    suspend fun allCollectionItems(): List<CollectionItemEntity>

    @Query(
        "SELECT duas.*, (favorites.dua_id IS NOT NULL) AS is_favorite FROM collection_items " +
            "JOIN duas ON duas.id = collection_items.dua_id " +
            "LEFT JOIN favorites ON favorites.dua_id = duas.id " +
            "WHERE collection_items.collection_id = :collectionId ORDER BY collection_items.added_at",
    )
    fun observeCollectionDuas(collectionId: Long): Flow<List<DuaWithFavorite>>

    @Query("SELECT collection_id FROM collection_items WHERE dua_id = :duaId")
    fun observeCollectionIdsContaining(duaId: String): Flow<List<Long>>

    @Insert
    suspend fun insertCollection(entity: CollectionEntity): Long

    @Query("UPDATE collections SET name = :name WHERE id = :collectionId")
    suspend fun renameCollection(collectionId: Long, name: String)

    @Query("DELETE FROM collections WHERE id = :collectionId")
    suspend fun deleteCollection(collectionId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollectionItem(entity: CollectionItemEntity)

    @Query("DELETE FROM collection_items WHERE collection_id = :collectionId AND dua_id = :duaId")
    suspend fun deleteCollectionItem(collectionId: Long, duaId: String)

    @Query("SELECT * FROM counters WHERE dua_id = :duaId")
    fun observeCounter(duaId: String): Flow<CounterEntity?>

    @Query("SELECT * FROM counters")
    suspend fun allCounters(): List<CounterEntity>

    @Upsert
    suspend fun upsertCounter(entity: CounterEntity)

    @Query("SELECT id FROM duas")
    suspend fun allDuaIds(): List<String>

    @Query("DELETE FROM favorites")
    suspend fun clearFavorites()

    @Query("DELETE FROM collections")
    suspend fun clearCollections()

    @Query("DELETE FROM counters")
    suspend fun clearCounters()
}
