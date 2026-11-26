package app.sukoon

import app.sukoon.data.backup.BackupCodec
import app.sukoon.data.backup.BackupCodec.applyBackup
import app.sukoon.data.backup.BackupCodec.toBackup
import app.sukoon.data.backup.BackupCollection
import app.sukoon.data.backup.BackupFavorite
import app.sukoon.data.backup.BackupFile
import app.sukoon.data.repository.PinHasher
import app.sukoon.data.seed.SeedFile
import app.sukoon.domain.model.ThemeMode
import app.sukoon.domain.model.UserSettings
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PinHasherTest {
    @Test fun verifiesCorrectPinOnly() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("2580", salt)
        assertTrue(PinHasher.verify("2580", salt, hash))
        assertFalse(PinHasher.verify("2581", salt, hash))
    }

    @Test fun saltChangesHash() {
        assertNotEquals(PinHasher.hash("1111", PinHasher.newSalt()), PinHasher.hash("1111", PinHasher.newSalt()))
    }
}

class BackupCodecTest {
    @Test fun roundTrip() {
        val file = BackupFile(
            createdAt = 1L,
            favorites = listOf(BackupFavorite("a", 2L)),
            collections = listOf(BackupCollection("Morning", 3L, listOf("a", "b"))),
            settings = UserSettings(themeMode = ThemeMode.AMOLED, arabicScale = 1.4f).toBackup(),
        )
        val decoded = BackupCodec.decode(BackupCodec.encode(file))
        assertEquals(file, decoded)
        val restored = UserSettings().applyBackup(decoded!!.settings!!)
        assertEquals(ThemeMode.AMOLED, restored.themeMode)
        assertEquals(1.4f, restored.arabicScale)
    }

    @Test fun rejectsForeignJson() {
        assertNull(BackupCodec.decode("""{"format":"something-else","version":1,"createdAt":0}"""))
        assertNull(BackupCodec.decode("not json"))
    }

    @Test fun clampsOutOfRangeScales() {
        val b = UserSettings().toBackup().copy(arabicScale = 9f)
        assertEquals(UserSettings.MAX_SCALE, UserSettings().applyBackup(b).arabicScale)
    }
}

/** Guards the bundled content: a broken seed would ship a broken app. */
class SeedDataValidationTest {
    private val seed: SeedFile = Json { ignoreUnknownKeys = true }
        .decodeFromString(SeedFile.serializer(), File("src/main/assets/duas.json").readText())
    private val arabic = Regex("[\\u0600-\\u06FF]")

    @Test fun idsAreUnique() {
        assertEquals(seed.duas.size, seed.duas.map { it.id }.toSet().size)
        assertEquals(seed.sections.size, seed.sections.map { it.id }.toSet().size)
    }

    @Test fun everySectionHasDuasAndEveryDuaHasASection() {
        val sectionIds = seed.sections.map { it.id }.toSet()
        assertEquals(10, sectionIds.size)
        seed.duas.forEach { assertTrue(it.id, it.sectionId in sectionIds) }
        sectionIds.forEach { id -> assertTrue(id, seed.duas.any { it.sectionId == id }) }
    }

    @Test fun recitableDuasAreComplete() {
        seed.duas.filter { it.kind == "DUA" }.forEach {
            assertTrue("${it.id} title", it.title.isNotBlank())
            assertTrue("${it.id} arabic", it.arabic.isNotBlank() && arabic.containsMatchIn(it.arabic))
            assertTrue("${it.id} transliteration", it.transliteration.isNotBlank() && !arabic.containsMatchIn(it.transliteration))
            assertTrue("${it.id} translation", it.translation.isNotBlank())
            assertTrue("${it.id} reference", it.reference.isNotBlank())
        }
    }

    @Test fun guidanceEntriesHaveText() {
        seed.duas.filter { it.kind == "GUIDANCE" }.forEach {
            assertTrue(it.translation.isNotBlank())
            assertTrue(it.reference.isNotBlank())
        }
    }

    @Test fun repeatCountsArePositive() {
        seed.duas.mapNotNull { it.repeatCount }.forEach { assertTrue(it > 0) }
    }

    @Test fun ordersAreContiguousPerSection() {
        seed.duas.groupBy { it.sectionId }.forEach { (s, list) ->
            assertEquals(s, (1..list.size).toList(), list.map { it.order }.sorted())
        }
    }
}
