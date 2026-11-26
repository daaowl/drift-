package app.sukoon

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.sukoon.data.local.CollectionEntity
import app.sukoon.data.local.CollectionItemEntity
import app.sukoon.data.local.DuaEntity
import app.sukoon.data.local.DuaFtsEntity
import app.sukoon.data.local.FavoriteEntity
import app.sukoon.data.local.Migrations
import app.sukoon.data.local.SectionEntity
import app.sukoon.data.local.SukoonDatabase
import app.sukoon.data.search.SearchNormalizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {
    private lateinit var db: SukoonDatabase

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SukoonDatabase::class.java).build()
    }

    @After fun tearDown() = db.close()

    private suspend fun seed() {
        db.contentDao().upsertSections(listOf(SectionEntity("solace", "Solace", "Grief", 1)))
        db.contentDao().upsertDuas(
            listOf(
                DuaEntity("solace.a", "solace", "Anxiety & Sorrow", "اللَّهُمَّ", "Allahumma inni 'abduka", "O Allah, I am Your servant", "Ahmad 3704", null, null, 1, "DUA"),
                DuaEntity("solace.b", "solace", "Complain", "إِنَّمَا", "Innama ashku", "I only complain", "Quran 12:86", null, 7, 2, "DUA"),
            ),
        )
        db.contentDao().insertFts(
            listOf(
                DuaFtsEntity("solace.a", SearchNormalizer.normalize("Anxiety & Sorrow Allahumma inni 'abduka servant Ahmad 3704")),
                DuaFtsEntity("solace.b", SearchNormalizer.normalize("Complain Innama ashku complain Quran 12:86")),
            ),
        )
    }

    @Test fun sectionCountsAndFavoriteFlag() = runTest {
        seed()
        assertEquals(2, db.duaDao().observeSections().first().single().duaCount)
        db.userDao().insertFavorite(FavoriteEntity("solace.b", 1))
        val list = db.duaDao().observeDuasInSection("solace").first()
        assertEquals(listOf(false, true), list.map { it.isFavorite })
    }

    @Test fun ftsSearchIsForgiving() = runTest {
        seed()
        val hits = db.duaDao().search(SearchNormalizer.toMatchQuery("anxi")!!).first()
        assertEquals(listOf("solace.a"), hits.map { it.dua.id })
        val ref = db.duaDao().search(SearchNormalizer.toMatchQuery("12:86")!!).first()
        assertEquals(listOf("solace.b"), ref.map { it.dua.id })
    }

    @Test fun contentUpsertKeepsFavourites() = runTest {
        seed()
        db.userDao().insertFavorite(FavoriteEntity("solace.a", 1))
        // A content update rewrites the row in place.
        db.contentDao().upsertDuas(
            listOf(DuaEntity("solace.a", "solace", "Anxiety", "اللَّهُمَّ", "x", "y", "Ahmad 3704", null, null, 1, "DUA")),
        )
        assertEquals(listOf("solace.a"), db.userDao().allFavorites().map { it.duaId })
    }

    @Test fun removingContentCascadesToUserRows() = runTest {
        seed()
        db.userDao().insertFavorite(FavoriteEntity("solace.b", 1))
        val c = db.userDao().insertCollection(CollectionEntity(name = "Night", createdAt = 1))
        db.userDao().insertCollectionItem(CollectionItemEntity(c, "solace.b", 1))
        db.contentDao().deleteDuasNotIn(listOf("solace.a"))
        assertTrue(db.userDao().allFavorites().isEmpty())
        assertTrue(db.userDao().allCollectionItems().isEmpty())
    }
}

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), SukoonDatabase::class.java)

    @Test fun latestSchemaOpensFromVersion1() {
        helper.createDatabase(DB, 1).close()
        val context = ApplicationProvider.getApplicationContext<Context>()
        Room.databaseBuilder(context, SukoonDatabase::class.java, DB)
            .addMigrations(*Migrations.ALL)
            .build()
            .apply { openHelper.writableDatabase.close() }
    }

    private companion object { const val DB = "migration-test" }
}
