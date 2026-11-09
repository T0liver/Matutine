package hu.toliver.matutine.store

import hu.toliver.matutine.edition.LocalEditionRepository
import hu.toliver.matutine.edition.ReaderSettings
import hu.toliver.matutine.edition.SourceList
import hu.toliver.matutine.edition.StoryRank
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileEditionStoreTest {
    @Test
    fun absentList_buildsSampleWithoutCreatingSourceList() = runBlocking {
        val dir = Files.createTempDirectory("matutine-absent").toFile()
        val store = FileEditionStore(dir.absolutePath)
        val edition = LocalEditionRepository(store)
            .loadCurrentMorningEdition()
            .getOrThrow()

        assertTrue(edition.isSample)
        assertEquals(StoryRank.lead, edition.stories.first().rank)
        assertNull(store.readSourceList())
        assertNull(store.readSettings())
        assertFalse(File(dir, "sources.json").exists())
        assertFalse(File(dir, "settings.json").exists())
    }

    @Test
    fun emptySourceList_doesNotBuildSample() = runBlocking {
        val dir = Files.createTempDirectory("matutine-empty").toFile()
        File(dir, "sources.json").writeText("""{"feeds":[],"calendars":[]}""")
        val store = FileEditionStore(dir.absolutePath)
        val result = LocalEditionRepository(store).loadCurrentMorningEdition()

        assertTrue(result.isFailure)
        assertTrue(store.readSourceList()?.feeds?.isEmpty() == true)
        assertFalse(File(dir, "editions").exists())
    }

    @Test
    fun writeSourceList_storesOneDocument() {
        val dir = Files.createTempDirectory("matutine-write-sources").toFile()
        val store = FileEditionStore(dir.absolutePath)
        store.writeSourceList(
            SourceList(
                id = "sources",
                revision = 1,
                feeds = listOf("https://example.com/rss.xml"),
                calendars = listOf("https://example.com/cal.ics"),
            ),
        )

        val file = File(dir, "sources.json")
        assertTrue(file.exists())
        assertFalse(file.readText().contains("Central banks"))
        assertEquals(listOf("https://example.com/rss.xml"), store.readSourceList()?.feeds)
        assertEquals(listOf("https://example.com/cal.ics"), store.readSourceList()?.calendars)
        assertEquals(1, store.readSourceList()?.revision)
    }

    @Test
    fun writeSettings_storesTopicsWithoutTouchingTheSourceList() {
        val dir = Files.createTempDirectory("matutine-settings").toFile()
        val store = FileEditionStore(dir.absolutePath)
        store.writeSourceList(
            SourceList(
                id = "sources",
                revision = 1,
                feeds = listOf("https://example.com/rss.xml"),
                calendars = listOf("https://example.com/cal.ics"),
            ),
        )

        store.writeSettings(ReaderSettings(topics = listOf("Technology", "Video games", "Rust", "school")))

        val settings = File(dir, "settings.json")
        assertTrue(settings.exists())
        assertEquals(
            listOf("Technology", "Video games", "Rust", "school"),
            store.readSettings()?.topics,
        )
        assertEquals(listOf("https://example.com/rss.xml"), store.readSourceList()?.feeds)
        assertEquals(listOf("https://example.com/cal.ics"), store.readSourceList()?.calendars)
        assertFalse(settings.readText().contains("https://example.com"))
    }

    @Test
    fun corruptSourceList_failsLoadAndDoesNotWriteSample() = runBlocking {
        val dir = Files.createTempDirectory("matutine-corrupt").toFile()
        File(dir, "sources.json").writeText("not json")
        val store = FileEditionStore(dir.absolutePath)

        val result = LocalEditionRepository(store).loadCurrentMorningEdition()

        assertTrue(result.isFailure)
        assertFalse(File(dir, "editions").exists())
        assertEquals("not json", File(dir, "sources.json").readText())
    }
}
