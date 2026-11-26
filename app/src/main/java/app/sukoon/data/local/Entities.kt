package app.sukoon.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)

@Entity(
    tableName = "duas",
    foreignKeys = [
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["section_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("section_id")],
)
data class DuaEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "section_id") val sectionId: String,
    val title: String,
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val reference: String,
    @ColumnInfo(name = "recitation_note") val recitationNote: String?,
    @ColumnInfo(name = "repeat_count") val repeatCount: Int?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    val kind: String,
)

@Fts4(notIndexed = ["dua_id"])
@Entity(tableName = "dua_fts")
data class DuaFtsEntity(
    @ColumnInfo(name = "dua_id") val duaId: String,
    val body: String,
)

@Entity(
    tableName = "favorites",
    foreignKeys = [
        ForeignKey(
            entity = DuaEntity::class,
            parentColumns = ["id"],
            childColumns = ["dua_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class FavoriteEntity(
    @PrimaryKey @ColumnInfo(name = "dua_id") val duaId: String,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "collection_items",
    primaryKeys = ["collection_id", "dua_id"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = DuaEntity::class,
            parentColumns = ["id"],
            childColumns = ["dua_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("dua_id")],
)
data class CollectionItemEntity(
    @ColumnInfo(name = "collection_id") val collectionId: Long,
    @ColumnInfo(name = "dua_id") val duaId: String,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)

@Entity(
    tableName = "counters",
    foreignKeys = [
        ForeignKey(
            entity = DuaEntity::class,
            parentColumns = ["id"],
            childColumns = ["dua_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CounterEntity(
    @PrimaryKey @ColumnInfo(name = "dua_id") val duaId: String,
    val count: Int,
    @ColumnInfo(name = "completed_rounds") val completedRounds: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

data class DuaWithFavorite(
    @Embedded val dua: DuaEntity,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean,
)

data class SectionWithCount(
    @Embedded val section: SectionEntity,
    @ColumnInfo(name = "dua_count") val duaCount: Int,
)

data class CollectionWithCount(
    @Embedded val collection: CollectionEntity,
    @ColumnInfo(name = "item_count") val itemCount: Int,
)
