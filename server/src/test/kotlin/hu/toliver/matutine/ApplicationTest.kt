package hu.toliver.matutine

import hu.toliver.matutine.edition.CalendarSnapshot
import hu.toliver.matutine.edition.DEFAULT_NOTIFICATION_TIME
import hu.toliver.matutine.edition.Edition
import hu.toliver.matutine.edition.EditionRole
import hu.toliver.matutine.edition.EditionStore
import hu.toliver.matutine.edition.EditionStory
import hu.toliver.matutine.edition.EditionTranscript
import hu.toliver.matutine.edition.FEED_DIDNT_LOAD
import hu.toliver.matutine.edition.ReaderSettings
import hu.toliver.matutine.edition.SourceList
import hu.toliver.matutine.edition.decodeEdition
import hu.toliver.matutine.edition.decodeReaderSettings
import hu.toliver.matutine.edition.parseNotificationTime
import hu.toliver.matutine.edition.todayLocalDate
import hu.toliver.matutine.edition.toDayKey
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ApplicationTest {

    @Test
    fun unsetToken_acceptsTheDemoBearer() = testApplication {
        val root = Files.createTempDirectory("matutine-demo").toFile()
        application {
            installTestDocuments(resolveSharedToken(null), DataDirectoryStore(root.absolutePath))
            module()
        }
        val response = client.get("/editions/current") {
            header("Authorization", "Bearer matutine-demo")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(decodeEdition(response.bodyAsText()).isSample)
    }

    @Test
    fun blankToken_isTheDemoBearer() {
        assertEquals("matutine-demo", resolveSharedToken(null))
        assertEquals("matutine-demo", resolveSharedToken(""))
        assertEquals("matutine-demo", resolveSharedToken("   "))
        assertEquals("/data", resolveDataRoot(null))
        assertEquals("/data", resolveDataRoot(""))
    }

    @Test
    fun setToken_rejectsEveryOtherBearerBeforeReadingDocuments() = testApplication {
        val store = TouchingStore()
        application {
            installTestDocuments(resolveSharedToken("lan-secret"), store)
            module()
        }
        val missing = client.get("/editions/current")
        val wrongScheme = client.get("/editions/current") {
            header("Authorization", "Token lan-secret")
        }
        val wrongToken = client.get("/editions/current") {
            header("Authorization", "Bearer matutine-demo")
        }
        assertEquals(HttpStatusCode.Unauthorized, missing.status)
        assertEquals(HttpStatusCode.Unauthorized, wrongScheme.status)
        assertEquals(HttpStatusCode.Unauthorized, wrongToken.status)
        assertFalse(missing.bodyAsText().contains("isSample"))
        assertFalse(wrongToken.bodyAsText().contains("mastheadName"))
        assertEquals(0, store.touches)
    }

    @Test
    fun setToken_acceptsThatBearer() = testApplication {
        val root = Files.createTempDirectory("matutine-set-token").toFile()
        application {
            installTestDocuments(resolveSharedToken("lan-secret"), DataDirectoryStore(root.absolutePath))
            module()
        }
        val accepted = client.get("/editions/current") {
            header("Authorization", "Bearer lan-secret")
        }
        assertEquals(HttpStatusCode.OK, accepted.status)
        assertTrue(decodeEdition(accepted.bodyAsText()).isSample)
    }

    @Test
    fun absentSourceList_returnsTheServerSampleAndDoesNotWriteTheList() = testApplication {
        val root = Files.createTempDirectory("matutine-sample").toFile()
        val store = DataDirectoryStore(root.absolutePath)
        application {
            installTestDocuments("matutine-demo", store)
            module()
        }
        val response = client.get("/editions/current") {
            header("Authorization", "Bearer matutine-demo")
        }
        val edition = decodeEdition(response.bodyAsText())
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(edition.isSample)
        assertTrue(EditionTranscript.render(edition).contains(EditionTranscript.SAMPLE))
        assertNull(store.readSourceList())
        assertFalse(root.resolve("sources.json").exists())
        assertTrue(root.resolve("editions/${edition.dayKey}-morning.json").exists())
        val again = decodeEdition(
            client.get("/editions/current") {
                header("Authorization", "Bearer matutine-demo")
            }.bodyAsText(),
        )
        assertEquals(edition.id, again.id)
        assertEquals(edition.revision, again.revision)
    }

    @Test
    fun existingMorning_isReturnedUnchanged() = testApplication {
        val root = Files.createTempDirectory("matutine-kept").toFile()
        val store = DataDirectoryStore(root.absolutePath)
        val day = todayLocalDate()
        val morning = Edition(
            id = "morning-${day.toDayKey()}",
            revision = 4,
            dayKey = day.toDayKey(),
            role = EditionRole.morning,
            mastheadName = "Matutine",
            dateLine = "Kept date",
            isSample = false,
            briefing = "Kept by hand",
            stories = emptyList(),
            events = emptyList(),
            tasks = emptyList(),
        )
        store.saveMorningEdition(morning)
        application {
            installTestDocuments("matutine-demo", store)
            module()
        }
        val response = client.get("/editions/current") {
            header("Authorization", "Bearer matutine-demo")
        }
        val edition = decodeEdition(response.bodyAsText())
        assertEquals(morning, edition)
        assertEquals(morning, store.loadMorningEdition(day.toDayKey()))
    }

    @Test
    fun documents_areFilesUnderTheDataDirectory() {
        val root = Files.createTempDirectory("matutine-docs").toFile()
        val store = DataDirectoryStore(root.absolutePath)
        store.writeSettings(ReaderSettings())
        store.writeSourceList(SourceList(id = "sources", revision = 1))
        store.writeCalendarSnapshot(CalendarSnapshot())
        val day = todayLocalDate()
        store.saveMorningEdition(
            Edition(
                id = "morning-${day.toDayKey()}",
                revision = 1,
                dayKey = day.toDayKey(),
                role = EditionRole.morning,
                mastheadName = "Matutine",
                dateLine = "Date",
                isSample = false,
                briefing = "Brief",
                stories = emptyList(),
                events = emptyList(),
                tasks = emptyList(),
            ),
        )
        assertTrue(root.resolve("settings.json").isFile)
        assertTrue(root.resolve("sources.json").isFile)
        assertTrue(root.resolve("calendar-snapshot.json").isFile)
        assertTrue(root.resolve("editions/${day.toDayKey()}-morning.json").isFile)
    }

    @Test
    fun blankGenerationTime_isSevenInTheProcessZone() {
        assertEquals(parseNotificationTime(DEFAULT_NOTIFICATION_TIME), resolveGenerationTime(null))
        assertEquals(parseNotificationTime(DEFAULT_NOTIFICATION_TIME), resolveGenerationTime("  "))
        assertNull(resolveGenerationTime("morning"))
        assertEquals(TimeZone.of(KIRITIMATI), resolveServerTimeZone(KIRITIMATI))
        assertEquals(TimeZone.currentSystemDefault(), resolveServerTimeZone("  "))
    }

    @Test
    fun dueMinute_absentList_insertsTheGeneratorDayKey() = runBlocking {
        val root = Files.createTempDirectory("matutine-due").toFile()
        val store = DataDirectoryStore(root.absolutePath)
        val clock = morningClock(store, DUE)
        clock.tick()
        val edition = decodeEdition(morningFile(root).readText())
        val zone = resolveServerTimeZone(KIRITIMATI)
        assertEquals(DUE.toLocalDateTime(zone).date.toDayKey(), edition.dayKey)
        assertEquals("2026-11-11", edition.dayKey)
        assertEquals("sample-2026-11-11", edition.id)
        assertTrue(edition.isSample)
        assertNull(store.readSourceList())
    }

    @Test
    fun existingMorning_isNotReplacedWhenTheClockFires() = runBlocking {
        val root = Files.createTempDirectory("matutine-keep-clock").toFile()
        val store = DataDirectoryStore(root.absolutePath)
        val dayKey = DUE.toLocalDateTime(resolveServerTimeZone(KIRITIMATI)).date.toDayKey()
        store.writeSourceList(SourceList(feeds = listOf("https://example.com/rss.xml")))
        store.saveMorningEdition(
            Edition(
                id = "morning-$dayKey",
                revision = 4,
                dayKey = dayKey,
                role = EditionRole.morning,
                mastheadName = "Matutine",
                dateLine = "Kept date",
                isSample = false,
                briefing = "Kept by hand",
                stories = emptyList(),
                events = emptyList(),
                tasks = emptyList(),
            ),
        )
        val before = morningFile(root).readText()
        var reads = 0
        val clock = morningClock(store, DUE, readFeed = {
            reads += 1
            PERSONAL_RSS
        })
        clock.tick()
        assertEquals(0, reads)
        assertEquals(before, morningFile(root).readText())
        assertEquals("Kept by hand", store.loadMorningEdition(dayKey)?.briefing)
    }

    @Test
    fun secondFire_doesNotWriteOrReadSources() = runBlocking {
        val root = Files.createTempDirectory("matutine-second").toFile()
        val store = DataDirectoryStore(root.absolutePath)
        store.writeSourceList(SourceList(feeds = listOf("https://example.com/rss.xml")))
        var reads = 0
        val clock = morningClock(store, DUE, readFeed = {
            reads += 1
            PERSONAL_RSS
        })
        clock.tick()
        val written = morningFile(root).readText()
        assertEquals(1, reads)
        clock.tick()
        assertEquals(1, reads)
        assertEquals(written, morningFile(root).readText())
    }

    @Test
    fun beforeTheMinute_doesNotInsert() = runBlocking {
        val root = Files.createTempDirectory("matutine-before").toFile()
        val store = DataDirectoryStore(root.absolutePath)
        morningClock(store, BEFORE).tick()
        assertFalse(morningFile(root).exists())
    }

    @Test
    fun afterTheMinute_doesNotCatchUp() = runBlocking {
        val root = Files.createTempDirectory("matutine-after").toFile()
        val store = DataDirectoryStore(root.absolutePath)
        morningClock(store, AFTER).tick()
        assertFalse(morningFile(root).exists())
    }

    @Test
    fun emptyOrUnreadableList_leavesTheStoreUnchanged() = runBlocking {
        listOf(SourceList(), SourceList(feeds = listOf("https://example.com/bad.xml"))).forEach { list ->
            val root = Files.createTempDirectory("matutine-bad-list").toFile()
            val store = DataDirectoryStore(root.absolutePath)
            store.writeSourceList(list)
            val before = root.resolve("sources.json").readText()
            morningClock(store, DUE, readFeed = { error(FEED_DIDNT_LOAD) }).tick()
            assertFalse(morningFile(root).exists())
            assertEquals(before, root.resolve("sources.json").readText())
        }
    }

    @Test
    fun geminiKey_isUsedInMemoryAndNotWritten() = runBlocking {
        val root = Files.createTempDirectory("matutine-key").toFile()
        val store = DataDirectoryStore(root.absolutePath)
        store.writeSettings(ReaderSettings(topics = listOf("ports")))
        store.writeSourceList(SourceList(feeds = listOf("https://example.com/rss.xml")))
        var seen = ""
        val clock = morningClock(
            store,
            DUE,
            geminiKey = "server-key",
            readFeed = { PERSONAL_RSS },
            summarize = { _, key ->
                seen = key
                "From the model."
            },
        )
        clock.tick()
        assertEquals("server-key", seen)
        assertEquals("From the model.", store.loadMorningEdition("2026-11-11")?.briefing)
        val settings = decodeReaderSettings(root.resolve("settings.json").readText())
        assertEquals("", settings.geminiKey)
        assertEquals(listOf("ports"), settings.topics)
        assertFalse(root.resolve("settings.json").readText().contains("server-key"))
    }

    @Test
    fun stoppedProcess_doesNotInsert() = runBlocking {
        val root = Files.createTempDirectory("matutine-stopped").toFile()
        val store = DataDirectoryStore(root.absolutePath)
        morningClock(store, DUE)
        assertFalse(morningFile(root).exists())
    }

    @Test
    fun runningServer_insertsOnTheGenerationMinute() = testApplication {
        val root = Files.createTempDirectory("matutine-running").toFile()
        val store = DataDirectoryStore(root.absolutePath)
        application {
            installTestDocuments("matutine-demo", store)
            installServerClock(
                zone = resolveServerTimeZone(KIRITIMATI),
                generationTime = "04:00",
                clock = { DUE },
            )
            module()
        }
        startApplication()
        val file = morningFile(root)
        val deadline = System.nanoTime() + 3_000_000_000L
        while (!file.exists() && System.nanoTime() < deadline) {
            delay(20)
        }
        assertTrue(file.exists())
        val edition = decodeEdition(file.readText())
        assertEquals("2026-11-11", edition.dayKey)
        val response = client.get("/editions/current") {
            header("Authorization", "Bearer matutine-demo")
        }
        val served = decodeEdition(response.bodyAsText())
        assertEquals(edition, served)
        assertEquals(1, served.revision)
    }
}

private const val KIRITIMATI = "Pacific/Kiritimati"
private val DUE: Instant = Instant.parse("2026-11-10T14:00:00Z")
private val BEFORE: Instant = Instant.parse("2026-11-10T13:59:00Z")
private val AFTER: Instant = Instant.parse("2026-11-10T14:01:00Z")

private const val PERSONAL_RSS = """
<?xml version="1.0"?>
<rss version="2.0">
  <channel>
    <title>Personal Wire</title>
    <item>
      <title>Markets open higher</title>
      <link>https://example.com/lead</link>
      <description>Traders watched the open.</description>
    </item>
  </channel>
</rss>
"""

private fun morningFile(root: File): File = root.resolve("editions/2026-11-11-morning.json")

private fun morningClock(
    store: EditionStore,
    instant: Instant,
    geminiKey: String = "",
    readFeed: suspend (String) -> String = { error(FEED_DIDNT_LOAD) },
    summarize: suspend (List<EditionStory>, String) -> String = { _, _ -> error("summarizer_unavailable") },
): ServerMorningClock {
    val zone = resolveServerTimeZone(KIRITIMATI)
    val (hour, minute) = requireNotNull(resolveGenerationTime("04:00"))
    val repository = serverEditionRepository(
        documents = store,
        zone = zone,
        clock = { instant },
        geminiKey = geminiKey,
        readFeed = readFeed,
        summarize = summarize,
    )
    return ServerMorningClock(
        zone = zone,
        hour = hour,
        minute = minute,
        clock = { instant },
        insert = { insertScheduledMorning(store, repository) },
    )
}

private class TouchingStore : EditionStore {
    var touches: Int = 0

    private fun touch(): Nothing {
        touches += 1
        error("store touched")
    }

    override fun readSourceList() = touch()
    override fun writeSourceList(sourceList: SourceList) = touch()
    override fun readSettings() = touch()
    override fun writeSettings(settings: ReaderSettings) = touch()
    override fun loadMorningEdition(dayKey: String) = touch()
    override fun saveMorningEdition(edition: Edition) = touch()
    override fun loadExtras(dayKey: String) = touch()
    override fun saveExtra(edition: Edition) = touch()
    override fun readCalendarSnapshot() = touch()
    override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) = touch()
    override fun readCalDavAccount() = touch()
    override fun writeCalDavAccount(account: hu.toliver.matutine.edition.CalDavAccount) = touch()
}
