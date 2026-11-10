package hu.toliver.matutine.edition

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RemoteEditionRepositoryTest {
    private val day = LocalDate(2026, 11, 10)
    private val serverEdition = SampleGenerator.forDay(day).copy(
        id = "morning-2026-11-10",
        revision = 4,
        isSample = false,
        briefing = "Extractive briefing.",
    )
    private val extra = serverEdition.copy(
        id = "extra-2026-11-10-1",
        revision = 1,
        role = EditionRole.extra,
        briefing = "An extra from the server.",
    )

    @Test
    fun httpLanUrlIsAcceptedAndOtherSchemesAreNot() {
        assertEquals("http://192.168.0.5:8080", normalizeServerBaseUrl("  http://192.168.0.5:8080/ "))
        assertEquals("https://paper.local", normalizeServerBaseUrl("https://paper.local"))
        assertNull(normalizeServerBaseUrl("ftp://192.168.0.5"))
        assertNull(normalizeServerBaseUrl("192.168.0.5:8080"))
        assertNull(normalizeServerBaseUrl(""))
        assertEquals("matutine-demo", presentedBearerToken("  "))
        assertEquals("lan-secret", presentedBearerToken(" lan-secret "))
    }

    @Test
    fun readShowsTheServerEditionAndDoesNotWriteTheLocalStore() = runBlocking {
        val store = MemoryStore()
        val local = LocalFake()
        val transport = ScriptedTransport { method, path ->
            assertEquals("GET", method)
            assertEquals("/editions/current", path)
            RemoteHttpResult(200, encodeEdition(serverEdition))
        }
        val repo = repository(store, local, transport, token = "")

        val loaded = repo.loadCurrentMorningEdition().getOrThrow()

        assertEquals(serverEdition, loaded)
        assertNull(repo.consumeLoadNotice())
        assertEquals(0, store.morningWrites)
        assertEquals(0, store.sourceWrites)
        assertEquals(0, store.snapshotWrites)
        assertEquals(listOf("GET /editions/current matutine-demo"), transport.calls)
        assertFalse(loaded.sourceNotes.any { it.contains("server") || it.contains("device") })
    }

    @Test
    fun wrongTokenKeepsTheStoredEditionAndTheTokenSentence() = runBlocking {
        val stored = serverEdition.copy(briefing = "Already on the phone.")
        val store = MemoryStore(morning = stored)
        val transport = ScriptedTransport { _, _ -> RemoteHttpResult(401, "unauthorized") }
        val repo = repository(store, LocalFake(), transport, token = "typed")

        val loaded = repo.loadCurrentMorningEdition().getOrThrow()

        assertEquals(stored, loaded)
        assertEquals(TOKEN_DOES_NOT_MATCH, repo.consumeLoadNotice())
        assertEquals(0, store.morningWrites)
        assertEquals(listOf("GET /editions/current typed"), transport.calls)
    }

    @Test
    fun offlineOpenShowsTheStoredEditionAndDoesNotUpload() = runBlocking {
        val stored = serverEdition.copy(briefing = "Copy on the phone.")
        val store = MemoryStore(morning = stored)
        val local = LocalFake()
        val transport = ScriptedTransport { _, _ -> error("offline") }
        val repo = repository(store, local, transport)

        val loaded = repo.loadCurrentMorningEdition().getOrThrow()

        assertEquals(stored, loaded)
        assertEquals(SERVER_COPY_ON_DEVICE, repo.consumeLoadNotice())
        assertEquals(0, store.morningWrites)
        assertEquals(0, store.sourceWrites)
        assertEquals(0, store.snapshotWrites)
        assertEquals(0, local.generateCalls)
        assertEquals(emptyList(), transport.calls.filter { it.startsWith("PUT") || it.startsWith("POST") })
    }

    @Test
    fun offlineOpenWithNoStoredEditionFailsWithTheSameSentence() = runBlocking {
        val transport = ScriptedTransport { _, _ -> error("offline") }
        val repo = repository(MemoryStore(), LocalFake(), transport)

        val loaded = repo.loadCurrentMorningEdition()

        assertEquals(SERVER_COPY_ON_DEVICE, loaded.exceptionOrNull()?.message)
        assertEquals(SERVER_COPY_ON_DEVICE, repo.consumeLoadNotice())
    }

    @Test
    fun reachableGenerateAndExtraRunOnTheServer() = runBlocking {
        val store = MemoryStore()
        val local = LocalFake()
        val transport = ScriptedTransport { method, path ->
            when (path) {
                "/editions/morning/regenerate" -> RemoteHttpResult(200, encodeEdition(serverEdition))
                "/editions" -> if (method == "POST") {
                    RemoteHttpResult(200, encodeEdition(extra))
                } else {
                    RemoteHttpResult(200, encodeEditionList(listOf(serverEdition, extra)))
                }
                else -> error(path)
            }
        }
        val repo = repository(store, local, transport)

        val regenerated = repo.generateToday().getOrThrow()
        val created = repo.newEditionNow().getOrThrow()
        val extras = repo.loadSameDayExtras()

        assertEquals(serverEdition.id, regenerated.id)
        assertEquals(EditionRole.extra, created.role)
        assertEquals(listOf(extra.id), extras.map { it.id })
        assertEquals(0, local.generateCalls)
        assertEquals(0, local.extraCalls)
        assertEquals(0, store.morningWrites)
        assertEquals(0, store.sourceWrites)
        assertEquals(0, store.snapshotWrites)
        assertEquals(
            listOf(
                "POST /editions/morning/regenerate secret",
                "POST /editions secret",
                "GET /editions secret",
            ),
            transport.calls,
        )
    }

    @Test
    fun unreachableGenerateWritesLocallyAndDoesNotPostSources() = runBlocking {
        val local = LocalFake()
        val transport = ScriptedTransport { _, _ -> error("offline") }
        val repo = repository(MemoryStore(), local, transport)

        val generated = repo.generateToday().getOrThrow()
        val extraResult = repo.newEditionNow()

        assertEquals("generated-local", generated.briefing)
        assertEquals(1, local.generateCalls)
        assertTrue(extraResult.isFailure)
        assertEquals(0, local.extraCalls)
        assertEquals(0, local.feedWrites)
        assertTrue(transport.calls.all { it.startsWith("POST /editions") })
        assertTrue(transport.calls.none { it.contains("/sources") || it.contains("calendar-snapshot") })
    }

    @Test
    fun remoteSettingsStayOnTheDeviceAndTheKeyIsNotSent() = runBlocking {
        val local = LocalFake()
        val transport = ScriptedTransport { _, _ -> error("unused") }
        val repo = repository(MemoryStore(), local, transport)

        assertEquals(Result.success(Unit), repo.saveNotificationTime("08:15"))
        assertEquals(Result.success(Unit), repo.saveNotificationBody("Ready when you are."))
        assertEquals(Result.success(Unit), repo.saveEditionTheme("newsprint"))
        assertEquals(Result.success(Unit), repo.saveEditionLayout("newsprint"))
        repo.saveTopics(listOf("Markets"))
        val savedFeed = repo.saveFeedUrl("https://example.com/rss.xml")
        assertEquals("", repo.savedGeminiKey())
        assertEquals(Result.success(Unit), repo.saveGeminiKey("should-not-land"))
        assertEquals(0, local.geminiWrites)
        assertEquals(1, local.notificationTimeWrites)
        assertEquals(1, local.topicWrites)
        assertEquals(1, local.feedWrites)
        assertEquals(Result.success(Unit), savedFeed)
        assertEquals(emptyList(), transport.calls)
        assertTrue(repo.updateMorningEdition(serverEdition).isFailure)
        assertEquals(emptyList(), transport.calls)
    }

    @Test
    fun typewriterSavePutsTheServerEditionAndDoesNotWriteLocally() = runBlocking {
        val store = MemoryStore()
        val saved = serverEdition.copy(briefing = "Typed on the desktop.", revision = 5)
        val savedExtra = extra.copy(briefing = "Typed extra.", revision = 2)
        var puts = 0
        val transport = ScriptedTransport { _, _ ->
            puts += 1
            val edition = if (puts == 1) saved else savedExtra
            RemoteHttpResult(200, encodeEdition(edition))
        }
        val repo = repository(store, LocalFake(), transport, writesEditions = true)

        val written = repo.updateMorningEdition(
            serverEdition.copy(briefing = "Typed on the desktop."),
        ).getOrThrow()
        val extraWritten = repo.updateExtra(extra.copy(briefing = "Typed extra.")).getOrThrow()

        assertEquals("Typed on the desktop.", written.briefing)
        assertEquals(5, written.revision)
        assertEquals("Typed extra.", extraWritten.briefing)
        assertEquals(0, store.morningWrites)
        assertEquals(0, store.sourceWrites)
        assertEquals(0, store.snapshotWrites)
        assertEquals(
            listOf("PUT /editions/by-id secret", "PUT /editions/by-id secret"),
            transport.calls,
        )
        assertTrue(transport.bodies[0].orEmpty().contains("Typed on the desktop."))
        assertTrue(transport.bodies[1].orEmpty().contains("Typed extra."))
    }

    @Test
    fun unreachableTypewriterDoesNotWriteLocally() = runBlocking {
        val store = MemoryStore()
        val transport = ScriptedTransport { _, _ -> error("offline") }
        val repo = repository(store, LocalFake(), transport, writesEditions = true)

        val written = repo.updateMorningEdition(serverEdition.copy(briefing = "Still here."))

        assertTrue(written.isFailure)
        assertEquals(EditionTranscript.SAVE_FAILED, written.exceptionOrNull()?.message)
        assertEquals(0, store.morningWrites)
        assertEquals(0, store.sourceWrites)
    }

    private fun repository(
        store: MemoryStore,
        local: LocalFake,
        transport: ScriptedTransport,
        token: String = "secret",
        writesEditions: Boolean = false,
    ) = RemoteEditionRepository(
        local = local,
        store = store,
        link = ServerLink(baseUrl = "http://192.168.0.5:8080", token = token),
        transport = transport,
        today = { day },
        writesEditions = writesEditions,
    )
}

private class ScriptedTransport(
    private val handler: (method: String, path: String) -> RemoteHttpResult,
) : RemoteEditionTransport {
    val calls = mutableListOf<String>()
    val bodies = mutableListOf<String?>()

    override suspend fun exchange(method: String, path: String, token: String, body: String?): RemoteHttpResult {
        calls += "$method $path $token"
        bodies += body
        return handler(method, path)
    }
}

private class MemoryStore(
    private val morning: Edition? = null,
) : EditionStore {
    var morningWrites = 0
    var sourceWrites = 0
    var snapshotWrites = 0

    override fun readSourceList(): SourceList? = null
    override fun writeSourceList(sourceList: SourceList) {
        sourceWrites += 1
    }
    override fun readSettings(): ReaderSettings? = null
    override fun writeSettings(settings: ReaderSettings) = Unit
    override fun loadMorningEdition(dayKey: String): Edition? = morning?.takeIf { it.dayKey == dayKey }
    override fun saveMorningEdition(edition: Edition) {
        morningWrites += 1
    }
    override fun loadExtras(dayKey: String): List<Edition> = emptyList()
    override fun saveExtra(edition: Edition) = Unit
    override fun readCalendarSnapshot(): CalendarSnapshot? = null
    override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) {
        snapshotWrites += 1
    }
    override fun readCalDavAccount(): CalDavAccount? = null
    override fun writeCalDavAccount(account: CalDavAccount) = Unit
    override fun readProcessConfig(): ServerProcessConfig? = null
    override fun writeProcessConfig(config: ServerProcessConfig) = Unit
}

private class LocalFake : EditionRepository {
    var generateCalls = 0
    var extraCalls = 0
    var feedWrites = 0
    var notificationTimeWrites = 0
    var topicWrites = 0
    var geminiWrites = 0

    override suspend fun loadCurrentMorningEdition(): Result<Edition> =
        Result.failure(IllegalStateException("local load is not used"))

    override suspend fun retrySample(): Result<Edition> =
        Result.failure(IllegalStateException("no sample"))

    override fun savedFeedUrls(): List<String> = emptyList()
    override suspend fun saveFeedUrl(url: String): Result<Unit> {
        feedWrites += 1
        return Result.success(Unit)
    }
    override fun savedCalendarUrls(): List<String> = emptyList()
    override suspend fun saveCalendarUrl(url: String): Result<Unit> = Result.success(Unit)
    override fun savedCalDavAccount(): CalDavAccount? = null
    override suspend fun saveCalDavAccount(url: String, username: String, password: String): Result<Unit> =
        Result.success(Unit)
    override fun savedTopics(): List<String> = emptyList()
    override fun saveTopics(topics: List<String>) {
        topicWrites += 1
    }
    override fun savedNotificationTime(): String = DEFAULT_NOTIFICATION_TIME
    override fun saveNotificationTime(time: String): Result<Unit> {
        notificationTimeWrites += 1
        return Result.success(Unit)
    }
    override fun savedNotificationBody(): String = EDITION_READY_NOTIFICATION_BODY
    override fun saveNotificationBody(body: String): Result<Unit> = Result.success(Unit)
    override fun savedEditionTheme(): String = DEFAULT_EDITION_THEME
    override fun saveEditionTheme(themeId: String): Result<Unit> = Result.success(Unit)
    override fun savedEditionLayout(): String = DEFAULT_EDITION_LAYOUT
    override fun saveEditionLayout(layoutId: String): Result<Unit> = Result.success(Unit)
    override fun savedGeminiKey(): String = "local-key"
    override fun saveGeminiKey(key: String): Result<Unit> {
        geminiWrites += 1
        return Result.success(Unit)
    }
    override suspend fun generateToday(): Result<Edition> {
        generateCalls += 1
        return Result.success(
            SampleGenerator.forDay(LocalDate(2026, 11, 10)).copy(briefing = "generated-local", isSample = false),
        )
    }
    override suspend fun newEditionNow(): Result<Edition> {
        extraCalls += 1
        return Result.failure(IllegalStateException("local extra"))
    }
    override suspend fun loadSameDayExtras(): List<Edition> = emptyList()
    override suspend fun updateMorningEdition(edition: Edition): Result<Edition> =
        Result.failure(IllegalStateException("local edit"))
    override suspend fun updateExtra(edition: Edition): Result<Edition> =
        Result.failure(IllegalStateException("local edit"))
}
