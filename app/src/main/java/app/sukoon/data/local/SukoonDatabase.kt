package app.sukoon.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        SectionEntity::class,
        DuaEntity::class,
        DuaFtsEntity::class,
        FavoriteEntity::class,
        CollectionEntity::class,
        CollectionItemEntity::class,
        CounterEntity::class,
    ],
    version = SukoonDatabase.VERSION,
    exportSchema = true,
)
abstract class SukoonDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
    abstract fun duaDao(): DuaDao
    abstract fun userDao(): UserDao

    companion object {
        const val VERSION = 1
        const val NAME = "sukoon.db"
    }
}

/**
 * Every schema change bumps [SukoonDatabase.VERSION] and adds a Migration here.
 * Destructive fallback is never enabled: user favourites and collections must survive upgrades.
 * Example for the next version:
 *
 * val MIGRATION_1_2 = object : Migration(1, 2) {
 *     override fun migrate(db: SupportSQLiteDatabase) {
 *         db.execSQL("ALTER TABLE duas ADD COLUMN translation_ur TEXT")
 *     }
 * }
 */
object Migrations {
    val ALL: Array<Migration> = emptyArray()
}
