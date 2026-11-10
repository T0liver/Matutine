package hu.toliver.matutine.edition

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeStore(
    private var morning: Edition? = null,
    private var sourceList: SourceList? = null,
    private var settings: ReaderSettings? = null,
    private var snapshot: CalendarSnapshot? = null,
    private var calDavAccount: CalDavAccount? = null,
    private var processConfig: ServerProcessConfig? = null,
) : EditionStore {
    var saved: Edition? = null
    var snapshotWrites: Int = 0
    var failSettingsWrite: Boolean = false

    override fun readSourceList(): SourceList? = sourceList

    override fun writeSourceList(sourceList: SourceList) {
        this.sourceList = sourceList
    }

    override fun readSettings(): ReaderSettings? = settings

    override fun writeSettings(settings: ReaderSettings) {
        if (failSettingsWrite) error("settings_unavailable")
        this.settings = settings
    }

    override fun loadMorningEdition(dayKey: String): Edition? =
        morning?.takeIf { it.dayKey == dayKey && it.role == EditionRole.morning }

    override fun saveMorningEdition(edition: Edition) {
        morning = edition
        saved = edition
    }

    private val extras = mutableListOf<Edition>()

    override fun loadExtras(dayKey: String): List<Edition> =
        extras.filter { it.dayKey == dayKey && it.role == EditionRole.extra }

    override fun saveExtra(edition: Edition) {
        require(edition.role == EditionRole.extra)
        val index = extras.indexOfFirst { it.id == edition.id }
        if (index >= 0) extras[index] = edition else extras += edition
    }

    fun seedExtra(edition: Edition) {
        extras += edition
    }

    override fun readCalendarSnapshot(): CalendarSnapshot? = snapshot

    override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) {
        this.snapshot = snapshot
        snapshotWrites += 1
    }

    override fun readCalDavAccount(): CalDavAccount? = calDavAccount

    override fun writeCalDavAccount(account: CalDavAccount) {
        calDavAccount = account
    }

    override fun readProcessConfig(): ServerProcessConfig? = processConfig

    override fun writeProcessConfig(config: ServerProcessConfig) {
        processConfig = config
    }
}

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
    <item>
      <title>Port reopens</title>
      <link>https://example.com/second</link>
      <description>Cargo is moving again</description>
    </item>
  </channel>
</rss>
"""

private const val PERSONAL_ATOM = """
<?xml version="1.0"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <title>Atom Desk</title>
  <entry>
    <title>Atom lead</title>
    <link rel="alternate" href="https://example.com/atom"/>
    <summary>Atom deck</summary>
  </entry>
</feed>
"""

private const val NOT_A_FEED = "<html><body>nope</body></html>"

private const val TOPIC_RSS = """
<?xml version="1.0"?>
<rss version="2.0">
  <channel>
    <title>Desk</title>
    <item>
      <title>Markets open higher</title>
      <link>https://example.com/markets</link>
      <description>Traders watched the open.</description>
    </item>
    <item>
      <title>A studio ships video games</title>
      <link>https://example.com/games</link>
      <description>Players lined up.</description>
    </item>
    <item>
      <title>TECHNOLOGY firms buy studios</title>
      <link>https://example.com/both</link>
      <description>A week of Video Games.</description>
    </item>
  </channel>
</rss>
"""

private const val TIE_RSS = """
<?xml version="1.0"?>
<rss version="2.0">
  <channel>
    <title>Desk</title>
    <item>
      <title>Studio ships a sequel</title>
      <link>https://example.com/games</link>
      <description>video games players lined up.</description>
    </item>
    <item>
      <title>Another studio follows</title>
      <link>https://example.com/games-two</link>
      <description>More video games news.</description>
    </item>
    <item>
      <title>Markets open higher</title>
      <link>https://example.com/markets</link>
      <description>Traders watched the open.</description>
    </item>
  </channel>
</rss>
"""

class LocalEditionRepositoryTest {
    private val sampleDay = LocalDate(2026, 11, 7)

    @Test
    fun absentSourceList_buildsBundledSampleAndDoesNotWriteList() = runBlocking {
        val store = FakeStore()
        val repo = LocalEditionRepository(store, today = { sampleDay })
        val edition = repo.loadCurrentMorningEdition().getOrThrow()
        val built = SampleGenerator.forDay(sampleDay)

        assertTrue(edition.isSample)
        assertEquals(built.stories.map { it.headline }, edition.stories.map { it.headline })
        assertEquals(built.events, edition.events)
        assertEquals(StoryRank.lead, edition.stories.first().rank)
        assertEquals(1, edition.stories.count { it.rank == StoryRank.lead })
        assertTrue(edition.briefing.endsWith("Summary assembled from headlines."))
        assertNull(store.readSourceList())
        assertNull(store.readSettings())
        assertEquals(edition.id, store.saved?.id)
    }

    @Test
    fun savedMorningEdition_isReturnedWithoutBuildingSample() = runBlocking {
        var generated = false
        val saved = SampleGenerator.forDay(sampleDay).copy(id = "saved", isSample = false, briefing = "Kept")
        val store = FakeStore(morning = saved)
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            generateSample = {
                generated = true
                error("sample")
            },
        )
        val edition = repo.loadCurrentMorningEdition().getOrThrow()
        assertEquals("saved", edition.id)
        assertEquals("Kept", edition.briefing)
        assertFalse(edition.isSample)
        assertFalse(generated)
        assertNull(store.saved)
    }

    @Test
    fun emptyOrPresentSourceList_doesNotUseBundledSample() = runBlocking {
        listOf(
            SourceList(),
            SourceList(feeds = listOf("https://example.com/feed.xml")),
        ).forEach { list ->
            var generated = false
            val repo = LocalEditionRepository(
                FakeStore(sourceList = list),
                today = { sampleDay },
                generateSample = {
                    generated = true
                    error("sample")
                },
            )
            val load = repo.loadCurrentMorningEdition()
            assertTrue(load.isFailure)
            assertEquals(MORNING_EDITION_NOT_READY, load.exceptionOrNull()?.message)
            assertTrue(repo.retrySample().isFailure)
            assertFalse(generated)
        }
    }

    @Test
    fun saveRssOrAtom_addsThatUrlAndLeavesTheSampleUntilGenerate() = runBlocking {
        val store = FakeStore()
        val documents = mapOf(
            "https://example.com/rss.xml" to PERSONAL_RSS,
            "https://example.com/atom.xml" to PERSONAL_ATOM,
        )
        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { url -> documents.getValue(url) })
        val sample = repo.loadCurrentMorningEdition().getOrThrow()

        repo.saveFeedUrl("https://example.com/rss.xml").getOrThrow()
        val rssList = requireNotNull(store.readSourceList())
        assertEquals(listOf("https://example.com/rss.xml"), rssList.feeds)
        assertTrue(rssList.calendars.isEmpty())
        assertFalse(encodeSourceList(rssList).contains("Central banks"))
        assertTrue(repo.loadCurrentMorningEdition().getOrThrow().isSample)
        assertEquals(sample.id, repo.loadCurrentMorningEdition().getOrThrow().id)

        repo.saveFeedUrl("  https://example.com/atom.xml  ").getOrThrow()
        assertEquals(listOf("https://example.com/atom.xml"), store.readSourceList()?.feeds)
        assertTrue(repo.loadCurrentMorningEdition().getOrThrow().isSample)
    }

    @Test
    fun saveFeedUrl_replacesFeedsAndKeepsTheCalendar() = runBlocking {
        val store = FakeStore(
            sourceList = SourceList(
                id = "sources",
                revision = 4,
                feeds = listOf("https://example.com/old.xml"),
                calendars = listOf("https://example.com/cal.ics"),
            ),
        )
        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { PERSONAL_RSS })

        repo.saveFeedUrl("https://example.com/rss.xml").getOrThrow()

        val list = requireNotNull(store.readSourceList())
        assertEquals(listOf("https://example.com/rss.xml"), list.feeds)
        assertEquals(listOf("https://example.com/cal.ics"), list.calendars)
    }

    @Test
    fun badDocumentOrFailedRead_keepsThePreviousUrlAndThePaper() = runBlocking {
        val morning = SampleGenerator.forDay(sampleDay)
        val store = FakeStore(
            morning = morning,
            sourceList = SourceList(id = "sources", revision = 2, feeds = listOf("https://example.com/old.xml"), calendars = listOf("https://example.com/cal.ics")),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            readFeed = { url ->
                if (url.contains("new")) NOT_A_FEED else error("offline")
            },
        )

        val badDocument = repo.saveFeedUrl("https://example.com/new.xml")
        assertTrue(badDocument.isFailure)
        assertEquals(FEED_DIDNT_LOAD, badDocument.exceptionOrNull()?.message)
        assertEquals(listOf("https://example.com/old.xml"), store.readSourceList()?.feeds)
        assertEquals(listOf("https://example.com/cal.ics"), store.readSourceList()?.calendars)
        assertEquals(morning.id, repo.loadCurrentMorningEdition().getOrThrow().id)

        val failedRead = repo.saveFeedUrl("https://example.com/also-new.xml")
        assertTrue(failedRead.isFailure)
        assertEquals(listOf("https://example.com/old.xml"), store.readSourceList()?.feeds)
        assertEquals(morning.id, repo.loadCurrentMorningEdition().getOrThrow().id)
    }

    @Test
    fun generate_savedFeed_buildsTodaysMorningEditionInSourceOrder() = runBlocking {
        val store = FakeStore()
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            readFeed = { PERSONAL_RSS },
        )
        val sample = repo.loadCurrentMorningEdition().getOrThrow()
        repo.saveFeedUrl("https://example.com/rss.xml").getOrThrow()

        val edition = repo.generateToday().getOrThrow()

        assertEquals("morning-${sampleDay.toDayKey()}", edition.id)
        assertEquals(1, edition.revision)
        assertEquals(sampleDay.toDayKey(), edition.dayKey)
        assertEquals(sampleDay.toDateLine(), edition.dateLine)
        assertEquals(EditionRole.morning, edition.role)
        assertEquals("Matutine", edition.mastheadName)
        assertFalse(edition.isSample)
        assertTrue(edition.events.isEmpty())
        assertTrue(edition.tasks.isEmpty())
        assertTrue(edition.sourceNotes.isEmpty())
        assertEquals(
            listOf("Markets open higher", "Port reopens"),
            edition.stories.map { it.headline },
        )
        assertEquals(listOf(StoryRank.lead, StoryRank.secondary), edition.stories.map { it.rank })
        assertEquals(1, edition.stories.count { it.rank == StoryRank.lead })
        assertEquals("Personal Wire", edition.stories.first().sourceName)
        assertEquals(
            "Traders watched the open. Cargo is moving again. Summary assembled from headlines.",
            edition.briefing,
        )
        assertTrue(edition.briefing.endsWith("Summary assembled from headlines."))
        assertEquals(listOf("https://example.com/rss.xml"), store.readSourceList()?.feeds)
        assertFalse(sample.isSample && edition.id == sample.id)
        assertEquals(edition.id, repo.loadCurrentMorningEdition().getOrThrow().id)
    }

    @Test
    fun generate_atomFeed_usesTheEntryAsTheLead() = runBlocking {
        val store = FakeStore(sourceList = SourceList(feeds = listOf("https://example.com/atom.xml")))
        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { PERSONAL_ATOM })

        val edition = repo.generateToday().getOrThrow()

        assertEquals("Atom lead", edition.stories.single().headline)
        assertEquals("Atom Desk", edition.stories.single().sourceName)
        assertEquals("https://example.com/atom", edition.stories.single().sourceUrl)
        assertEquals("Atom deck.", edition.briefing.removeSuffix(" Summary assembled from headlines."))
        assertEquals(StoryRank.lead, edition.stories.single().rank)
        assertFalse(edition.isSample)
    }

    @Test
    fun generate_mixedFeeds_omitsTheFailureAndNotesIt() = runBlocking {
        val store = FakeStore(
            sourceList = SourceList(
                feeds = listOf(
                    "https://example.com/bad-first.xml",
                    "https://example.com/rss.xml",
                    "https://example.com/bad-second.xml",
                ),
            ),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            readFeed = { url -> if (url.contains("bad")) NOT_A_FEED else PERSONAL_RSS },
        )

        val edition = repo.generateToday().getOrThrow()

        assertEquals(listOf("Markets open higher", "Port reopens"), edition.stories.map { it.headline })
        assertEquals(StoryRank.lead, edition.stories.first().rank)
        assertEquals(listOf(FEED_DIDNT_LOAD, FEED_DIDNT_LOAD), edition.sourceNotes)
        assertFalse(edition.isSample)
        assertEquals(
            listOf(
                "https://example.com/bad-first.xml",
                "https://example.com/rss.xml",
                "https://example.com/bad-second.xml",
            ),
            store.readSourceList()?.feeds,
        )
    }

    @Test
    fun generate_whenNothingParses_keepsTheListAndTheCurrentPaper() = runBlocking {
        val morning = SampleGenerator.forDay(sampleDay)
        val list = SourceList(feeds = listOf("https://example.com/bad.xml"))
        val store = FakeStore(morning = morning, sourceList = list)
        var generatedSample = false
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            generateSample = {
                generatedSample = true
                error("sample")
            },
            readFeed = { NOT_A_FEED },
        )

        val result = repo.generateToday()

        assertTrue(result.isFailure)
        assertEquals(FEED_DIDNT_LOAD, result.exceptionOrNull()?.message)
        assertFalse(generatedSample)
        assertEquals(list, store.readSourceList())
        assertEquals(morning.id, repo.loadCurrentMorningEdition().getOrThrow().id)
        assertTrue(repo.loadCurrentMorningEdition().getOrThrow().isSample)
        assertNull(store.saved)
    }

    @Test
    fun generate_emptyOrAbsentList_doesNotBuildTheBundledSample() = runBlocking {
        listOf<SourceList?>(null, SourceList()).forEach { list ->
            var generatedSample = false
            val store = FakeStore(sourceList = list)
            val repo = LocalEditionRepository(
                store,
                today = { sampleDay },
                generateSample = {
                    generatedSample = true
                    error("sample")
                },
            )
            assertTrue(repo.generateToday().isFailure)
            assertFalse(generatedSample)
            assertNull(store.saved)
            assertEquals(list, store.readSourceList())
        }
    }

    @Test
    fun generate_again_keepsThePersonalIdAndIncrementsRevision() = runBlocking {
        val existing = SampleGenerator.forDay(sampleDay).copy(
            id = "morning-${sampleDay.toDayKey()}",
            revision = 3,
            isSample = false,
        )
        val store = FakeStore(
            morning = existing,
            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
        )
        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { PERSONAL_RSS })

        val edition = repo.generateToday().getOrThrow()

        assertEquals(existing.id, edition.id)
        assertEquals(4, edition.revision)
        assertFalse(edition.isSample)
    }

    @Test
    fun generate_again_keepsBriefingRewrittenLineAndTasks() = runBlocking {
        val existing = SampleGenerator.forDay(sampleDay).copy(
            id = "morning-${sampleDay.toDayKey()}",
            revision = 3,
            isSample = false,
            briefing = "Kept by hand",
            events = listOf(
                EditionEvent("Planning", "9:30"),
                EditionEvent("Team standup", "10:00"),
            ),
            tasks = listOf(
                EditionTask("Buy milk", completed = true),
                EditionTask("File notes"),
            ),
        )
        val store = FakeStore(
            morning = existing,
            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
            snapshot = CalendarSnapshot(
                occurrences = listOf(
                    CalendarOccurrence(title = "Planning", startEpochSecond = utc(9, 30), endEpochSecond = utc(10, 0)),
                ),
            ),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            timeZone = TimeZone.UTC,
            readFeed = { PERSONAL_RSS },
        )

        val edition = repo.generateToday().getOrThrow()

        assertEquals(existing.id, edition.id)
        assertEquals(4, edition.revision)
        assertEquals("Kept by hand", edition.briefing)
        assertEquals(existing.tasks, edition.tasks)
        assertTrue(edition.events.contains(EditionEvent("Planning", "9:30")))
        assertTrue(edition.events.contains(EditionEvent("Team standup", "10:00")))
        assertEquals(1, edition.events.count { it == EditionEvent("Planning", "9:30") })
        assertEquals(
            listOf("Markets open higher", "Port reopens"),
            edition.stories.map { it.headline },
        )
        assertEquals(StoryRank.lead, edition.stories.first().rank)
    }

    @Test
    fun newEditionNow_insertsAnExtraAndLeavesOtherEditions() = runBlocking {
        val morning = SampleGenerator.forDay(sampleDay).copy(
            id = "morning-${sampleDay.toDayKey()}",
            revision = 2,
            isSample = false,
            briefing = "Morning stays",
        )
        val older = morning.copy(
            id = "extra-${sampleDay.toDayKey()}-1",
            revision = 1,
            role = EditionRole.extra,
            briefing = "Older extra",
        )
        val store = FakeStore(
            morning = morning,
            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
        )
        store.seedExtra(older)
        store.seedExtra(older.copy(id = "extra-2026-11-06-1", dayKey = "2026-11-06", briefing = "Yesterday"))
        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { PERSONAL_RSS })

        val extra = repo.newEditionNow().getOrThrow()

        assertEquals(EditionRole.extra, extra.role)
        assertEquals("extra-${sampleDay.toDayKey()}-2", extra.id)
        assertEquals(1, extra.revision)
        assertEquals(morning, store.loadMorningEdition(sampleDay.toDayKey()))
        assertEquals(older, store.loadExtras(sampleDay.toDayKey()).first())
        assertEquals(morning, repo.loadCurrentMorningEdition().getOrThrow())
        assertEquals(listOf(older, extra), repo.loadSameDayExtras())
        assertTrue(repo.loadSameDayExtras().none { it.dayKey != sampleDay.toDayKey() })
    }

    @Test
    fun lateSnapshot_isOnRegenerateOrExtraAndNotOnThePaperBefore() = runBlocking {
        val morning = SampleGenerator.forDay(sampleDay).copy(
            id = "morning-${sampleDay.toDayKey()}",
            revision = 1,
            isSample = false,
            events = listOf(EditionEvent("Kept", "8:00")),
        )
        val later = CalendarOccurrence(title = "Later", startEpochSecond = utc(15, 0), endEpochSecond = utc(16, 0))
        val store = FakeStore(
            morning = morning,
            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
            snapshot = CalendarSnapshot(occurrences = listOf(later)),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            timeZone = TimeZone.UTC,
            readFeed = { PERSONAL_RSS },
        )

        assertEquals(listOf(EditionEvent("Kept", "8:00")), repo.loadCurrentMorningEdition().getOrThrow().events)

        val regenerated = repo.generateToday().getOrThrow()
        assertTrue(regenerated.events.contains(EditionEvent("Later", "15:00")))
        assertEquals(morning.id, regenerated.id)

        val untouched = regenerated.copy(events = listOf(EditionEvent("Kept", "8:00")), revision = regenerated.revision)
        store.saveMorningEdition(untouched)
        val extra = repo.newEditionNow().getOrThrow()
        assertEquals(untouched, store.loadMorningEdition(sampleDay.toDayKey()))
        assertTrue(extra.events.contains(EditionEvent("Later", "15:00")))
        assertFalse(extra.events.contains(EditionEvent("Kept", "8:00")))
    }

    @Test
    fun failedGenerateAndExtra_leaveStoredEditions() = runBlocking {
        val morning = SampleGenerator.forDay(sampleDay).copy(
            id = "morning-${sampleDay.toDayKey()}",
            revision = 2,
            isSample = false,
            briefing = "Still here",
        )
        val store = FakeStore(
            morning = morning,
            sourceList = SourceList(feeds = listOf("https://example.com/bad.xml")),
        )
        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { NOT_A_FEED })

        val generated = repo.generateToday()
        val extra = repo.newEditionNow()

        assertTrue(generated.isFailure)
        assertEquals(FEED_DIDNT_LOAD, generated.exceptionOrNull()?.message)
        assertTrue(extra.isFailure)
        assertEquals(FEED_DIDNT_LOAD, extra.exceptionOrNull()?.message)
        assertEquals(morning, store.loadMorningEdition(sampleDay.toDayKey()))
        assertTrue(store.loadExtras(sampleDay.toDayKey()).isEmpty())
    }

    @Test
    fun saveTopics_storesChoicesLinesAndLeavesTheSourceList() = runBlocking {
        val list = SourceList(
            id = "sources",
            revision = 2,
            feeds = listOf("https://example.com/rss.xml"),
            calendars = listOf("https://example.com/cal.ics"),
        )
        val store = FakeStore(sourceList = list)
        val repo = LocalEditionRepository(store, today = { sampleDay })
        val topics = topicsFromInterests(
            selectedChoices = listOf("Technology", "Video games"),
            freeLine = "Rust",
            dayLines = listOf("school", " "),
        )

        repo.saveTopics(topics)

        assertEquals(listOf("Technology", "Video games", "Rust", "school"), topics)
        assertEquals(topics, store.readSettings()?.topics)
        assertEquals(list, store.readSourceList())
    }

    @Test
    fun generate_withTopics_putsTheBestFitFirstAsTheOnlyLead() = runBlocking {
        val store = FakeStore(sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")))
        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { TOPIC_RSS })
        repo.saveTopics(
            topicsFromInterests(
                selectedChoices = listOf("Technology", "Video games"),
                freeLine = "Rust",
                dayLines = listOf("school", ""),
            ),
        )

        val edition = repo.generateToday().getOrThrow()

        assertEquals(
            listOf("TECHNOLOGY firms buy studios", "A studio ships video games", "Markets open higher"),
            edition.stories.map { it.headline },
        )
        assertEquals(
            listOf(StoryRank.lead, StoryRank.secondary, StoryRank.note),
            edition.stories.map { it.rank },
        )
        assertEquals(1, edition.stories.count { it.rank == StoryRank.lead })
        assertTrue(edition.briefing.startsWith("Traders watched the open."))
        assertFalse(encodeEdition(edition).contains("\"fit\""))
    }

    @Test
    fun generate_equalTopicFits_keepSourceOrder() = runBlocking {
        val store = FakeStore(sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")))
        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { TIE_RSS })
        repo.saveTopics(listOf("Video games"))

        val edition = repo.generateToday().getOrThrow()

        assertEquals(
            listOf("Studio ships a sequel", "Another studio follows", "Markets open higher"),
            edition.stories.map { it.headline },
        )
        assertEquals(StoryRank.lead, edition.stories.first().rank)
        assertEquals(1, edition.stories.count { it.rank == StoryRank.lead })
    }

    @Test
    fun generate_absentOrEmptyTopics_keepsSourceOrderOnThePersonsEdition() = runBlocking {
        listOf<ReaderSettings?>(null, ReaderSettings(topics = emptyList())).forEach { settings ->
            val store = FakeStore(
                sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
                settings = settings,
            )
            val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { PERSONAL_RSS })

            val edition = repo.generateToday().getOrThrow()

            assertEquals(
                listOf("Markets open higher", "Port reopens"),
                edition.stories.map { it.headline },
            )
            assertEquals(StoryRank.lead, edition.stories.first().rank)
            assertFalse(edition.isSample)
            assertEquals(settings, store.readSettings())
        }
    }

    @Test
    fun clearTopics_nextGenerateUsesSourceOrderAndDoesNotReturnTheSample() = runBlocking {
        val store = FakeStore(
            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
            settings = ReaderSettings(topics = listOf("Technology", "Video games")),
        )
        val repo = LocalEditionRepository(store, today = { sampleDay }, readFeed = { PERSONAL_RSS })

        repo.saveTopics(
            topicsFromInterests(
                selectedChoices = emptyList(),
                freeLine = "  ",
                dayLines = listOf("", "   "),
            ),
        )
        val edition = repo.generateToday().getOrThrow()

        assertEquals(emptyList(), store.readSettings()?.topics)
        assertEquals(
            listOf("Markets open higher", "Port reopens"),
            edition.stories.map { it.headline },
        )
        assertEquals(listOf(StoryRank.lead, StoryRank.secondary), edition.stories.map { it.rank })
        assertFalse(edition.isSample)
        assertFalse(edition.id.startsWith("sample-"))
    }

    @Test
    fun editionWithoutSourceNotes_decodesAsEmpty() {
        val json = """
            {"id":"sample-2026-11-07","revision":1,"dayKey":"2026-11-07","role":"morning","mastheadName":"Matutine","dateLine":"Saturday, November 7, 2026","isSample":true,"briefing":"Kept","stories":[],"events":[],"tasks":[]}
        """.trimIndent()
        val edition = decodeEdition(json)
        assertTrue(edition.sourceNotes.isEmpty())
        assertTrue(encodeEdition(edition).contains("\"sourceNotes\":[]"))
    }

    @Test
    fun documentThatIsNeitherRssNorAtom_failsWithTheFeedSentence() {
        val failure = runCatching { parseFeedDocument(NOT_A_FEED) }
        assertTrue(failure.isFailure)
        assertEquals(FEED_DIDNT_LOAD, failure.exceptionOrNull()?.message)
        assertEquals("Atom lead", parseFeedDocument(PERSONAL_ATOM).single().headline)
    }

    @Test
    fun generate_icsEvents_showTitleAndStartTimeInOrder() = runBlocking {
        val store = FakeStore(sourceList = SourceList(feeds = listOf("https://example.com/rss.xml"), calendars = listOf("https://example.com/cal.ics")))
        val documents = mapOf(
            "https://example.com/rss.xml" to PERSONAL_RSS,
            "https://example.com/cal.ics" to TODAY_ICS,
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            timeZone = TimeZone.UTC,
            readFeed = { url -> documents.getValue(url) },
        )

        val edition = repo.generateToday().getOrThrow()

        assertEquals(
            listOf(
                EditionEvent("Planning", "9:30"),
                EditionEvent("Lunch", "12:30"),
                EditionEvent("Solo", "15:00"),
            ),
            edition.events,
        )
        assertEquals(listOf("Markets open higher", "Port reopens"), edition.stories.map { it.headline })
        assertEquals(StoryRank.lead, edition.stories.first().rank)
        assertTrue(edition.tasks.isEmpty())
        assertTrue(edition.briefing.isNotBlank())
    }

    @Test
    fun generate_duplicateStartEndAndTitle_appearsOnce() = runBlocking {
        val duplicate = CalendarOccurrence(
            title = "Planning",
            startEpochSecond = utc(9, 30),
            endEpochSecond = utc(10, 0),
        )
        val store = FakeStore(
            sourceList = SourceList(
                feeds = listOf("https://example.com/rss.xml"),
                calendars = listOf("https://example.com/cal.ics"),
            ),
            snapshot = CalendarSnapshot(occurrences = listOf(duplicate.copy(title = " Planning "))),
        )
        val documents = mapOf(
            "https://example.com/rss.xml" to PERSONAL_RSS,
            "https://example.com/cal.ics" to DUPLICATE_ICS,
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            timeZone = TimeZone.UTC,
            readFeed = { url -> documents.getValue(url) },
        )

        val edition = repo.generateToday().getOrThrow()

        assertEquals(listOf(EditionEvent("Planning", "9:30")), edition.events)
    }

    @Test
    fun saveCalendarUrl_whenTheLinkDoesNotOpen_keepsThePriorCalendar() = runBlocking {
        val list = SourceList(
            id = "sources",
            revision = 3,
            feeds = listOf("https://example.com/rss.xml"),
            calendars = listOf("https://example.com/old.ics"),
        )
        val store = FakeStore(sourceList = list)
        var reads = 0
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            readFeed = { url ->
                reads += 1
                if (url.startsWith("https://user:secret@")) error("must not fetch credentials")
                if (url.contains("closed")) error("offline")
                NOT_A_FEED
            },
        )

        val closed = repo.saveCalendarUrl("https://example.com/closed.ics")
        assertTrue(closed.isFailure)
        assertEquals(CALENDAR_LINK_DIDNT_OPEN, closed.exceptionOrNull()?.message)
        assertEquals(list, store.readSourceList())

        val notACalendar = repo.saveCalendarUrl("https://example.com/page.html")
        assertTrue(notACalendar.isFailure)
        assertEquals(CALENDAR_LINK_DIDNT_OPEN, notACalendar.exceptionOrNull()?.message)
        assertEquals(list, store.readSourceList())

        val password = repo.saveCalendarUrl("https://user:secret@example.com/cal.ics")
        assertTrue(password.isFailure)
        assertEquals(CALENDAR_LINK_DIDNT_OPEN, password.exceptionOrNull()?.message)
        assertEquals(list, store.readSourceList())
        assertEquals(2, reads)
    }

    @Test
    fun saveCalendarUrl_replacesCalendarsAndKeepsFeeds() = runBlocking {
        val store = FakeStore(
            sourceList = SourceList(
                id = "sources",
                revision = 2,
                feeds = listOf("https://example.com/rss.xml"),
                calendars = listOf("https://example.com/old.ics", "https://example.com/other.ics"),
            ),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            readFeed = { TODAY_ICS },
        )

        repo.saveCalendarUrl("  https://example.com/new.ics  ").getOrThrow()

        val saved = requireNotNull(store.readSourceList())
        assertEquals(listOf("https://example.com/rss.xml"), saved.feeds)
        assertEquals(listOf("https://example.com/new.ics"), saved.calendars)
        assertEquals(3, saved.revision)
    }

    @Test
    fun generate_deviceEvents_areIncludedAndStored() = runBlocking {
        val device = CalendarOccurrence(title = "Device standup", startEpochSecond = utc(8, 0), endEpochSecond = utc(8, 30))
        val store = FakeStore(sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")))
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            timeZone = TimeZone.UTC,
            writesDeviceCalendarSnapshot = true,
            readDeviceCalendar = { DeviceCalendarRead.Events(listOf(device)) },
            readFeed = { PERSONAL_RSS },
        )

        val edition = repo.generateToday().getOrThrow()

        assertEquals(listOf(EditionEvent("Device standup", "8:00")), edition.events)
        assertEquals(CalendarSnapshot(occurrences = listOf(device)), store.readCalendarSnapshot())
        assertEquals(1, store.snapshotWrites)
        assertEquals(listOf("Markets open higher", "Port reopens"), edition.stories.map { it.headline })
    }

    @Test
    fun generate_failedOrEmptyDeviceRead_keepsTheSnapshot() = runBlocking {
        val kept = CalendarOccurrence(title = "Kept", startEpochSecond = utc(8, 0), endEpochSecond = utc(8, 15))
        listOf(DeviceCalendarRead.Failed, DeviceCalendarRead.Empty, DeviceCalendarRead.Unavailable, DeviceCalendarRead.Events(emptyList())).forEach { read ->
            val store = FakeStore(
                sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
                snapshot = CalendarSnapshot(occurrences = listOf(kept)),
            )
            val repo = LocalEditionRepository(
                store,
                today = { sampleDay },
                timeZone = TimeZone.UTC,
                writesDeviceCalendarSnapshot = true,
                readDeviceCalendar = { read },
                readFeed = { PERSONAL_RSS },
            )

            val edition = repo.generateToday().getOrThrow()

            assertEquals(CalendarSnapshot(occurrences = listOf(kept)), store.readCalendarSnapshot())
            assertEquals(0, store.snapshotWrites)
            assertEquals(listOf(EditionEvent("Kept", "8:00")), edition.events)
        }
    }

    @Test
    fun recordDeviceCalendar_afterThePaperExists_doesNotChangeTheEdition() = runBlocking {
        val paper = SampleGenerator.forDay(sampleDay).copy(
            id = "morning-${sampleDay.toDayKey()}",
            isSample = false,
            events = listOf(EditionEvent("Kept", "8:00")),
        )
        val later = CalendarOccurrence(title = "Later", startEpochSecond = utc(15, 0), endEpochSecond = utc(16, 0))
        val store = FakeStore(
            morning = paper,
            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            writesDeviceCalendarSnapshot = true,
            readDeviceCalendar = { DeviceCalendarRead.Events(listOf(later)) },
        )

        repo.recordDeviceCalendar()

        assertEquals(listOf(EditionEvent("Kept", "8:00")), repo.loadCurrentMorningEdition().getOrThrow().events)
        assertEquals(paper, repo.loadCurrentMorningEdition().getOrThrow())
        assertNull(store.saved)
        assertEquals(CalendarSnapshot(occurrences = listOf(later)), store.readCalendarSnapshot())
    }

    @Test
    fun generate_withoutAndroidFlag_doesNotReadOrWriteTheDevice() = runBlocking {
        val kept = CalendarOccurrence(title = "Kept", startEpochSecond = utc(8, 0), endEpochSecond = utc(8, 15))
        val store = FakeStore(
            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
            snapshot = CalendarSnapshot(occurrences = listOf(kept)),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            timeZone = TimeZone.UTC,
            writesDeviceCalendarSnapshot = false,
            readDeviceCalendar = { error("desktop must not read the device calendar") },
            readFeed = { PERSONAL_RSS },
        )

        val edition = repo.generateToday().getOrThrow()

        assertEquals(0, store.snapshotWrites)
        assertEquals(CalendarSnapshot(occurrences = listOf(kept)), store.readCalendarSnapshot())
        assertEquals(listOf(EditionEvent("Kept", "8:00")), edition.events)
    }

    @Test
    fun icsOccurrences_keepTodayDropTodosAndUseAMissingEndAsTheStart() {
        val occurrences = parseIcsOccurrences(TODAY_ICS, TimeZone.UTC)
        assertEquals(listOf("Lunch", "Yesterday", "Planning", "Solo"), occurrences.map { it.title })
        assertEquals(
            listOf(
                EditionEvent("Planning", "9:30"),
                EditionEvent("Lunch", "12:30"),
                EditionEvent("Solo", "15:00"),
            ),
            editionEventsFrom(listOf(occurrences), sampleDay, TimeZone.UTC),
        )
        val solo = occurrences.single { it.title == "Solo" }
        assertEquals(solo.startEpochSecond, solo.endEpochSecond)
        val allDay = parseIcsOccurrences(
            """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Away
            DTSTART;VALUE=DATE:20261107
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:Yesterday away
            DTSTART;VALUE=DATE:20261106
            END:VEVENT
            END:VCALENDAR
            """.trimIndent(),
            TimeZone.UTC,
        )
        assertEquals(
            listOf(EditionEvent("Away", "0:00")),
            editionEventsFrom(listOf(allDay), sampleDay, TimeZone.UTC),
        )
        assertFalse(calendarUrlSendsPassword("https://example.com/cal.ics"))
        assertTrue(calendarUrlSendsPassword("https://user:secret@example.com/cal.ics"))
        val mixed = parseIcsOccurrences(
            """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            SUMMARY:Timed
            DTSTART;VALUE=DATE-TIME:20261107T093000Z
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:Impossible
            DTSTART:20261131T093000Z
            END:VEVENT
            BEGIN:VEVENT
            SUMMARY:Leap second
            DTSTART:20261107T093060Z
            END:VEVENT
            END:VCALENDAR
            """.trimIndent(),
            TimeZone.UTC,
        )
        assertEquals(listOf(EditionEvent("Timed", "9:30")), editionEventsFrom(listOf(mixed), sampleDay, TimeZone.UTC))
        assertFalse(mixed.single().allDay)
    }

    @Test
    fun generate_failedIcsFetch_keepsSnapshotEvents() = runBlocking {
        val kept = CalendarOccurrence(title = "Kept", startEpochSecond = utc(8, 0), endEpochSecond = utc(8, 15))
        val store = FakeStore(
            sourceList = SourceList(
                feeds = listOf("https://example.com/rss.xml"),
                calendars = listOf("https://example.com/cal.ics"),
            ),
            snapshot = CalendarSnapshot(occurrences = listOf(kept)),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            timeZone = TimeZone.UTC,
            readFeed = { url ->
                if (url.endsWith(".ics")) error("offline")
                PERSONAL_RSS
            },
        )

        val edition = repo.generateToday().getOrThrow()

        assertEquals(listOf(EditionEvent("Kept", "8:00")), edition.events)
        assertEquals(listOf("Markets open higher", "Port reopens"), edition.stories.map { it.headline })
        assertEquals(CalendarSnapshot(occurrences = listOf(kept)), store.readCalendarSnapshot())
    }

    @Test
    fun generate_userinfoCalendarUrl_isNotFetched() = runBlocking {
        val fetched = mutableListOf<String>()
        val store = FakeStore(
            sourceList = SourceList(
                feeds = listOf("https://example.com/rss.xml"),
                calendars = listOf("https://user:secret@example.com/cal.ics"),
            ),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            timeZone = TimeZone.UTC,
            readFeed = { url ->
                fetched += url
                if (url.contains('@')) error("must not fetch credentials")
                PERSONAL_RSS
            },
        )

        val edition = repo.generateToday().getOrThrow()

        assertEquals(listOf("https://example.com/rss.xml"), fetched)
        assertTrue(edition.events.isEmpty())
        assertEquals(
            listOf("https://user:secret@example.com/cal.ics"),
            store.readSourceList()?.calendars,
        )
    }

    @Test
    fun generate_calDavEvents_joinIcsAndDeviceEvents() = runBlocking {
        val device = CalendarOccurrence(title = "Device standup", startEpochSecond = utc(8, 0), endEpochSecond = utc(8, 30))
        val store = FakeStore(
            sourceList = SourceList(
                feeds = listOf("https://example.com/rss.xml"),
                calendars = listOf("https://example.com/cal.ics"),
            ),
            snapshot = CalendarSnapshot(occurrences = listOf(device)),
            calDavAccount = CalDavAccount(
                url = "https://calendar.example/dav/personal/",
                username = "o",
                password = "local-secret",
            ),
        )
        var calDavReads = 0
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            timeZone = TimeZone.UTC,
            readFeed = { url ->
                if (url.endsWith(".ics")) TODAY_ICS else PERSONAL_RSS
            },
            readCalDav = { account ->
                calDavReads += 1
                assertEquals("o", account.username)
                assertEquals("local-secret", account.password)
                assertFalse(account.url.contains("@"))
                CALDAV_LATER
            },
        )

        val edition = repo.generateToday().getOrThrow()

        assertEquals(1, calDavReads)
        assertEquals(
            listOf(
                EditionEvent("Device standup", "8:00"),
                EditionEvent("Planning", "9:30"),
                EditionEvent("Lunch", "12:30"),
                EditionEvent("Solo", "15:00"),
                EditionEvent("CalDAV review", "16:00"),
            ),
            edition.events,
        )
        assertTrue(edition.sourceNotes.isEmpty())
        assertEquals(listOf("Markets open higher", "Port reopens"), edition.stories.map { it.headline })
    }

    @Test
    fun generate_sameStartEndAndTitleAcrossSources_appearsOnce() = runBlocking {
        val duplicate = CalendarOccurrence(
            title = "Planning",
            startEpochSecond = utc(9, 30),
            endEpochSecond = utc(10, 0),
        )
        val store = FakeStore(
            sourceList = SourceList(
                feeds = listOf("https://example.com/rss.xml"),
                calendars = listOf("https://example.com/cal.ics"),
            ),
            snapshot = CalendarSnapshot(occurrences = listOf(duplicate.copy(title = " Planning "))),
            calDavAccount = CalDavAccount("https://calendar.example/dav/personal/", "o", "local-secret"),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            timeZone = TimeZone.UTC,
            readFeed = { url -> if (url.endsWith(".ics")) DUPLICATE_ICS else PERSONAL_RSS },
            readCalDav = { DUPLICATE_ICS },
        )

        val edition = repo.generateToday().getOrThrow()

        assertEquals(listOf(EditionEvent("Planning", "9:30")), edition.events)
    }

    @Test
    fun generate_failedCalDavRead_notesTheFailureAndKeepsTheEdition() = runBlocking {
        val store = FakeStore(
            sourceList = SourceList(
                feeds = listOf("https://example.com/rss.xml"),
                calendars = listOf("https://example.com/cal.ics"),
            ),
            calDavAccount = CalDavAccount("https://calendar.example/dav/personal/", "o", "local-secret"),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            timeZone = TimeZone.UTC,
            readFeed = { url -> if (url.endsWith(".ics")) TODAY_ICS else PERSONAL_RSS },
            readCalDav = { error("offline") },
        )

        val edition = repo.generateToday().getOrThrow()

        assertEquals(
            listOf(
                EditionEvent("Planning", "9:30"),
                EditionEvent("Lunch", "12:30"),
                EditionEvent("Solo", "15:00"),
            ),
            edition.events,
        )
        assertEquals(listOf(CALDAV_DIDNT_LOAD), edition.sourceNotes)
        assertEquals(listOf("Markets open higher", "Port reopens"), edition.stories.map { it.headline })
        assertEquals(edition, store.saved)
    }

    @Test
    fun generate_calDavResponseWithVtodo_keepsOnlyVevents() = runBlocking {
        val store = FakeStore(
            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
            calDavAccount = CalDavAccount("https://calendar.example/dav/personal/", "o", "local-secret"),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            timeZone = TimeZone.UTC,
            readFeed = { PERSONAL_RSS },
            readCalDav = { CALDAV_WITH_VTODO },
        )

        val edition = repo.generateToday().getOrThrow()

        assertEquals(listOf(EditionEvent("CalDAV review", "16:00")), edition.events)
        assertTrue(edition.sourceNotes.isEmpty())
    }

    @Test
    fun load_doesNotCallCalDav() = runBlocking {
        val morning = SampleGenerator.forDay(sampleDay).copy(
            id = "morning-${sampleDay.toDayKey()}",
            isSample = false,
        )
        val store = FakeStore(
            morning = morning,
            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
            calDavAccount = CalDavAccount("https://calendar.example/dav/personal/", "o", "local-secret"),
        )
        var calls = 0
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            readCalDav = {
                calls += 1
                error("must not call")
            },
        )

        val edition = repo.loadCurrentMorningEdition().getOrThrow()

        assertEquals(morning.id, edition.id)
        assertEquals(0, calls)
    }

    @Test
    fun saveCalDavAccount_storesThePasswordOutsideTheSourceListAndDoesNotFetch() = runBlocking {
        val store = FakeStore(
            sourceList = SourceList(
                id = "sources",
                revision = 1,
                feeds = listOf("https://example.com/rss.xml"),
                calendars = listOf("https://example.com/cal.ics"),
            ),
        )
        var calls = 0
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            readCalDav = {
                calls += 1
                error("must not fetch")
            },
        )

        repo.saveCalDavAccount(
            "  https://calendar.example/dav/personal/  ",
            " o ",
            "local-secret",
        ).getOrThrow()

        val account = requireNotNull(store.readCalDavAccount())
        assertEquals("https://calendar.example/dav/personal/", account.url)
        assertEquals("o", account.username)
        assertEquals("local-secret", account.password)
        assertEquals("", repo.savedCalDavAccount()?.password)
        assertEquals(listOf("https://example.com/rss.xml"), store.readSourceList()?.feeds)
        assertEquals(listOf("https://example.com/cal.ics"), store.readSourceList()?.calendars)
        assertFalse(encodeSourceList(requireNotNull(store.readSourceList())).contains("local-secret"))
        assertEquals(0, calls)

        val rejected = repo.saveCalDavAccount("https://o:local-secret@calendar.example/dav/", "o", "local-secret")
        assertTrue(rejected.isFailure)
        assertEquals(account, store.readCalDavAccount())
        assertEquals(0, calls)

        val cleartext = repo.saveCalDavAccount("http://calendar.example/dav/personal/", "o", "local-secret")
        assertTrue(cleartext.isFailure)
        assertEquals(account, store.readCalDavAccount())

        val kept = repo.saveCalDavAccount("https://calendar.example/dav/personal/", "o", "")
        assertTrue(kept.isSuccess)
        assertEquals("local-secret", store.readCalDavAccount()?.password)

        val renamed = repo.saveCalDavAccount("https://calendar.example/dav/personal/", "other", "")
        assertTrue(renamed.isFailure)
        assertEquals(account, store.readCalDavAccount())
        assertEquals(0, calls)
    }

    @Test
    fun generate_nonCalendarCalDavBody_notesTheFailure() = runBlocking {
        val store = FakeStore(
            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
            calDavAccount = CalDavAccount("https://calendar.example/dav/personal/", "o", "local-secret"),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            timeZone = TimeZone.UTC,
            readFeed = { PERSONAL_RSS },
            readCalDav = { "not a calendar" },
        )

        val edition = repo.generateToday().getOrThrow()

        assertTrue(edition.events.isEmpty())
        assertEquals(listOf(CALDAV_DIDNT_LOAD), edition.sourceNotes)
    }

    @Test
    fun generate_calDavErrorStatusWithoutEvents_notesTheFailure() = runBlocking {
        val store = FakeStore(
            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
            calDavAccount = CalDavAccount("https://calendar.example/dav/personal/", "o", "local-secret"),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            timeZone = TimeZone.UTC,
            readFeed = { PERSONAL_RSS },
            readCalDav = {
                """
                <D:multistatus xmlns:D="DAV:">
                  <D:response><D:status>HTTP/1.1 404 Not Found</D:status></D:response>
                </D:multistatus>
                """.trimIndent()
            },
        )

        val edition = repo.generateToday().getOrThrow()

        assertEquals(listOf(CALDAV_DIDNT_LOAD), edition.sourceNotes)
    }

    @Test
    fun runMorning_absentFeed_insertsOnceAndKeepsThatPaper() = runBlocking {
        val store = FakeStore(sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")))
        val probe = SourceProbe()
        val repo = morningRepo(store, probe)

        val inserted = repo.runMorning().getOrThrow()
        val edition = requireNotNull(inserted)

        assertEquals("2026-11-07", edition.dayKey)
        assertEquals(sampleDay.toDayKey(), edition.dayKey)
        assertEquals("morning-${sampleDay.toDayKey()}", edition.id)
        assertEquals(1, edition.revision)
        assertEquals(EditionRole.morning, edition.role)
        assertFalse(edition.isSample)
        assertEquals(listOf("Markets open higher", "Port reopens"), edition.stories.map { it.headline })
        assertEquals(StoryRank.lead, edition.stories.first().rank)
        assertEquals(edition, store.saved)
        assertEquals(1, probe.feedReads)
        assertEquals(0, probe.deviceReads)
        assertEquals(0, probe.calDavReads)

        val again = repo.runMorning().getOrThrow()

        assertNull(again)
        assertEquals(edition, store.saved)
        assertEquals(1, probe.feedReads)
        assertEquals(0, probe.deviceReads)
        assertEquals(0, probe.calDavReads)
        assertEquals(edition.id, repo.loadCurrentMorningEdition().getOrThrow().id)
    }

    @Test
    fun runMorning_savedSampleOrEdition_doesNotWriteOrReadSources() = runBlocking {
        listOf(true, false).forEach { sample ->
            val kept = SampleGenerator.forDay(sampleDay).copy(
                id = if (sample) "sample-${sampleDay.toDayKey()}" else "morning-${sampleDay.toDayKey()}",
                isSample = sample,
                briefing = "Kept",
            )
            val store = FakeStore(
                morning = kept,
                sourceList = SourceList(
                    feeds = listOf("https://example.com/rss.xml"),
                    calendars = listOf("https://example.com/cal.ics"),
                ),
                calDavAccount = CalDavAccount("https://calendar.example/dav/personal/", "o", "local-secret"),
            )
            val probe = SourceProbe()
            val repo = morningRepo(store, probe, deviceCalendar = true)

            val result = repo.runMorning().getOrThrow()

            assertNull(result)
            assertNull(store.saved)
            assertEquals(0, store.snapshotWrites)
            assertEquals(0, probe.feedReads)
            assertEquals(0, probe.deviceReads)
            assertEquals(0, probe.calDavReads)
            assertEquals(kept, repo.loadCurrentMorningEdition().getOrThrow())
        }
    }

    @Test
    fun runMorning_serverConfigured_doesNotInsertOrReadSources() = runBlocking {
        val store = FakeStore(sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")))
        val probe = SourceProbe()
        val repo = morningRepo(store, probe, serverConfigured = true)

        val result = repo.runMorning().getOrThrow()

        assertNull(result)
        assertNull(store.saved)
        assertEquals(0, probe.feedReads)
        assertEquals(0, probe.deviceReads)
        assertEquals(0, probe.calDavReads)
        assertNull(store.loadMorningEdition(sampleDay.toDayKey()))
    }

    @Test
    fun runMorning_missingOrUnreadableFeed_leavesTheStoreUnchanged() = runBlocking {
        listOf<SourceList?>(null, SourceList(), SourceList(feeds = listOf("https://example.com/bad.xml"))).forEach { list ->
            val store = FakeStore(sourceList = list)
            val probe = SourceProbe()
            val repo = morningRepo(store, probe, document = NOT_A_FEED)

            val result = repo.runMorning()

            assertTrue(result.isFailure)
            assertEquals(FEED_DIDNT_LOAD, result.exceptionOrNull()?.message)
            assertNull(store.saved)
            assertEquals(list, store.readSourceList())
            assertNull(store.loadMorningEdition(sampleDay.toDayKey()))
            assertEquals(0, probe.deviceReads)
            assertEquals(0, probe.calDavReads)
        }
    }

    @Test
    fun runMorning_failedFeedBesideAGoodOne_omitsItAndNotesIt() = runBlocking {
        val store = FakeStore(
            sourceList = SourceList(
                feeds = listOf(
                    "https://example.com/bad-first.xml",
                    "https://example.com/rss.xml",
                ),
            ),
        )
        val probe = SourceProbe()
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            timeZone = TimeZone.UTC,
            readFeed = { url ->
                probe.feedReads += 1
                if (url.contains("bad")) NOT_A_FEED else PERSONAL_RSS
            },
            readDeviceCalendar = {
                probe.deviceReads += 1
                DeviceCalendarRead.Unavailable
            },
            readCalDav = {
                probe.calDavReads += 1
                error("must not call")
            },
        )

        val edition = requireNotNull(repo.runMorning().getOrThrow())

        assertEquals(listOf("Markets open higher", "Port reopens"), edition.stories.map { it.headline })
        assertEquals(listOf(FEED_DIDNT_LOAD), edition.sourceNotes)
        assertEquals(edition, store.saved)
        assertEquals("2026-11-07", edition.dayKey)
    }

    @Test
    fun generateToday_stillReplacesTheEditionAMorningRunLeftInPlace() = runBlocking {
        val existing = SampleGenerator.forDay(sampleDay).copy(
            id = "morning-${sampleDay.toDayKey()}",
            revision = 3,
            isSample = false,
        )
        val store = FakeStore(
            morning = existing,
            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
        )
        val probe = SourceProbe()
        val repo = morningRepo(store, probe)

        assertNull(repo.runMorning().getOrThrow())
        assertEquals(0, probe.feedReads)
        assertNull(store.saved)

        val replaced = repo.generateToday().getOrThrow()

        assertEquals(existing.id, replaced.id)
        assertEquals(4, replaced.revision)
        assertEquals("2026-11-07", replaced.dayKey)
        assertFalse(replaced.isSample)
        assertEquals(replaced, store.saved)
        assertEquals(listOf("Markets open higher", "Port reopens"), replaced.stories.map { it.headline })
    }

    @Test
    fun runMorning_paperThatAppearsDuringTheRead_isNotReplaced() = runBlocking {
        val kept = SampleGenerator.forDay(sampleDay).copy(
            id = "morning-${sampleDay.toDayKey()}",
            isSample = false,
            briefing = "Kept",
        )
        val inner = FakeStore(sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")))
        var feeds = 0
        var devices = 0
        val store = object : EditionStore by inner {
            override fun loadMorningEdition(dayKey: String): Edition? =
                if (feeds == 0) inner.loadMorningEdition(dayKey) else kept.takeIf { it.dayKey == dayKey }
        }
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            timeZone = TimeZone.UTC,
            writesDeviceCalendarSnapshot = true,
            readFeed = {
                feeds += 1
                PERSONAL_RSS
            },
            readDeviceCalendar = {
                devices += 1
                error("must not read")
            },
            readCalDav = { error("must not call") },
        )

        val result = repo.runMorning().getOrThrow()

        assertEquals(kept, result)
        assertNull(inner.saved)
        assertEquals(1, feeds)
        assertEquals(0, devices)
    }

    @Test
    fun runMorning_keepsTheDayCapturedAtTheStart() = runBlocking {
        val nextDay = sampleDay.plus(1, DateTimeUnit.DAY)
        var calls = 0
        val store = FakeStore(sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")))
        val repo = LocalEditionRepository(
            store,
            today = {
                val day = if (calls == 0) sampleDay else nextDay
                calls += 1
                day
            },
            timeZone = TimeZone.UTC,
            readFeed = { PERSONAL_RSS },
        )

        val edition = requireNotNull(repo.runMorning().getOrThrow())

        assertEquals(1, calls)
        assertEquals("2026-11-07", edition.dayKey)
        assertEquals("2026-11-07", store.saved?.dayKey)
        assertNull(store.loadMorningEdition(nextDay.toDayKey()))
    }

    private fun morningRepo(
        store: FakeStore,
        probe: SourceProbe,
        document: String = PERSONAL_RSS,
        deviceCalendar: Boolean = false,
        serverConfigured: Boolean = false,
    ) = LocalEditionRepository(
        store,
        today = { sampleDay },
        timeZone = TimeZone.UTC,
        writesDeviceCalendarSnapshot = deviceCalendar,
        readFeed = {
            probe.feedReads += 1
            document
        },
        readDeviceCalendar = {
            probe.deviceReads += 1
            DeviceCalendarRead.Unavailable
        },
        readCalDav = {
            probe.calDavReads += 1
            error("must not call")
        },
        serverConfigured = serverConfigured,
    )

    @Test
    fun calDavReport_asksForVeventAndKeepsThePasswordOutOfTheBody() {
        val document = calDavReportDocument()
        assertTrue(document.contains("VEVENT"))
        assertFalse(document.contains("VTODO"))
        val header = basicAuthorizationValue("o", "local-secret")
        assertTrue(header.startsWith("Basic "))
        assertFalse(document.contains("local-secret"))
        assertFalse(header.contains("local-secret"))
        assertEquals(
            "o:local-secret",
            kotlin.io.encoding.Base64.decode(header.removePrefix("Basic ")).decodeToString(),
        )
    }
}

private class SourceProbe {
    var feedReads: Int = 0
    var deviceReads: Int = 0
    var calDavReads: Int = 0
}

private fun utc(hour: Int, minute: Int): Long =
    LocalDateTime(2026, 11, 7, hour, minute, 0).toInstant(TimeZone.UTC).epochSeconds

private const val TODAY_ICS = """
BEGIN:VCALENDAR
BEGIN:VEVENT
SUMMARY:Lunch
DTSTART:20261107T123000Z
DTEND:20261107T130000Z
END:VEVENT
BEGIN:VTODO
SUMMARY:Buy milk
DTSTART:20261107T070000Z
END:VTODO
BEGIN:VEVENT
SUMMARY:Yesterday
DTSTART:20261106T093000Z
DTEND:20261106T100000Z
END:VEVENT
BEGIN:VEVENT
SUMMARY:Planning
DTSTART:20261107T093000Z
DTEND:20261107T100000Z
END:VEVENT
BEGIN:VEVENT
SUMMARY:Solo
DTSTART:20261107T150000Z
END:VEVENT
END:VCALENDAR
"""

private const val CALDAV_LATER = """
BEGIN:VCALENDAR
BEGIN:VEVENT
SUMMARY:CalDAV review
DTSTART:20261107T160000Z
DTEND:20261107T163000Z
END:VEVENT
END:VCALENDAR
"""

private const val CALDAV_WITH_VTODO = """
<D:multistatus xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
  <D:response>
    <D:propstat>
      <D:prop>
        <C:calendar-data>BEGIN:VCALENDAR
BEGIN:VTODO
SUMMARY:Buy milk
DTSTART:20261107T070000Z
END:VTODO
BEGIN:VEVENT
SUMMARY:CalDAV review
DTSTART:20261107T160000Z
DTEND:20261107T163000Z
END:VEVENT
END:VCALENDAR</C:calendar-data>
      </D:prop>
    </D:propstat>
  </D:response>
</D:multistatus>
"""

private const val DUPLICATE_ICS = """
BEGIN:VCALENDAR
BEGIN:VEVENT
SUMMARY:Planning
DTSTART:20261107T093000Z
DTEND:20261107T100000Z
END:VEVENT
BEGIN:VEVENT
SUMMARY: Planning 
DTSTART:20261107T093000Z
DTEND:20261107T100000Z
END:VEVENT
END:VCALENDAR
"""

class EditionTaskPersistenceTest {
    private val sampleDay = LocalDate(2026, 11, 10)

    @Test
    fun updateMorningEdition_bumpsRevisionAndPersistsTasks() = runBlocking {
        val morning = SampleGenerator.forDay(sampleDay).copy(
            id = "morning-2026-11-10",
            revision = 3,
            isSample = false,
            tasks = listOf(EditionTask("Buy milk")),
        )
        val store = FakeStore(morning = morning)
        val repo = LocalEditionRepository(store, today = { sampleDay })

        val updated = morning.copy(tasks = morning.tasks.map { it.copy(completed = true) })
        val saved = repo.updateMorningEdition(updated).getOrThrow()

        assertEquals(4, saved.revision)
        assertTrue(saved.tasks.single().completed)
        assertEquals(saved, store.saved)
    }

    @Test
    fun updateMorningEdition_rejectsWrongDayOrId() = runBlocking {
        val morning = SampleGenerator.forDay(sampleDay).copy(id = "morning-2026-11-10", revision = 1, isSample = false)
        val store = FakeStore(morning = morning)
        val repo = LocalEditionRepository(store, today = { sampleDay })
        val otherDay = morning.copy(dayKey = "2026-11-09")
        assertTrue(repo.updateMorningEdition(otherDay).isFailure)
        val otherId = morning.copy(id = "other")
        assertTrue(repo.updateMorningEdition(otherId).isFailure)
    }

    @Test
    fun generate_withKey_storesOneModelBriefingOnANewMorning() = runBlocking {
        var calls = 0
        val store = FakeStore(
            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
            settings = ReaderSettings(geminiKey = "configured"),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            readFeed = { PERSONAL_RSS },
            summarize = { stories, key ->
                calls += 1
                assertEquals("configured", key)
                assertEquals("Markets open higher", stories.first().headline)
                "The harbor is quiet."
            },
        )

        val edition = repo.generateToday().getOrThrow()

        assertEquals("The harbor is quiet.", edition.briefing)
        assertFalse(edition.briefing.contains("Summary assembled from headlines."))
        assertFalse(edition.briefing.contains("gemini", ignoreCase = true))
        assertEquals(1, calls)
        assertFalse(edition.isSample)
    }

    @Test
    fun generate_withKey_replacesASampleWithOneModelBriefing() = runBlocking {
        var calls = 0
        val store = FakeStore()
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            readFeed = { PERSONAL_RSS },
            summarize = { _, _ ->
                calls += 1
                "A written opening."
            },
        )
        val sample = repo.loadCurrentMorningEdition().getOrThrow()
        assertTrue(sample.isSample)
        assertEquals(0, calls)
        store.writeSettings(ReaderSettings(geminiKey = "configured"))
        repo.saveFeedUrl("https://example.com/rss.xml").getOrThrow()

        val edition = repo.generateToday().getOrThrow()

        assertEquals("A written opening.", edition.briefing)
        assertFalse(edition.isSample)
        assertEquals(1, calls)
    }

    @Test
    fun generate_withoutKey_keepsTheExtractiveBriefingAndDoesNotCall() = runBlocking {
        var calls = 0
        listOf<ReaderSettings?>(null, ReaderSettings(geminiKey = "  ")).forEach { settings ->
            val store = FakeStore(
                sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
                settings = settings,
            )
            val repo = LocalEditionRepository(
                store,
                today = { sampleDay },
                readFeed = { PERSONAL_RSS },
                summarize = { _, _ ->
                    calls += 1
                    "Should not be stored."
                },
            )

            val edition = repo.generateToday().getOrThrow()

            assertTrue(edition.briefing.endsWith("Summary assembled from headlines."))
            assertEquals(edition, store.saved)
        }
        assertEquals(0, calls)
    }

    @Test
    fun generate_whenTheCallFailsOrIsBlank_keepsTheExtractiveBriefing() = runBlocking {
        listOf<() -> String>({ error("summarizer_unavailable") }, { "  " }).forEach { result ->
            var calls = 0
            val store = FakeStore(
                sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
                settings = ReaderSettings(geminiKey = "configured"),
            )
            val repo = LocalEditionRepository(
                store,
                today = { sampleDay },
                readFeed = { PERSONAL_RSS },
                summarize = { _, _ ->
                    calls += 1
                    result()
                },
            )

            val edition = repo.generateToday().getOrThrow()

            assertTrue(edition.briefing.endsWith("Summary assembled from headlines."))
            assertEquals(1, calls)
            assertEquals(edition, store.saved)
        }
    }

    @Test
    fun generate_again_withKey_keepsTheBriefingAndDoesNotCall() = runBlocking {
        var calls = 0
        val existing = SampleGenerator.forDay(sampleDay).copy(
            id = "morning-${sampleDay.toDayKey()}",
            revision = 3,
            isSample = false,
            briefing = "Kept by hand",
        )
        val store = FakeStore(
            morning = existing,
            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
            settings = ReaderSettings(geminiKey = "configured"),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            readFeed = { PERSONAL_RSS },
            summarize = { _, _ ->
                calls += 1
                "A new opening."
            },
        )

        val edition = repo.generateToday().getOrThrow()

        assertEquals("Kept by hand", edition.briefing)
        assertEquals(0, calls)
    }

    @Test
    fun newEditionNow_withKey_storesOneModelBriefingOnTheExtra() = runBlocking {
        var calls = 0
        val morning = SampleGenerator.forDay(sampleDay).copy(
            id = "morning-${sampleDay.toDayKey()}",
            revision = 2,
            isSample = false,
            briefing = "Morning stays",
        )
        val store = FakeStore(
            morning = morning,
            sourceList = SourceList(feeds = listOf("https://example.com/rss.xml")),
            settings = ReaderSettings(geminiKey = "configured"),
        )
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            readFeed = { PERSONAL_RSS },
            summarize = { _, _ ->
                calls += 1
                "Extra opening."
            },
        )

        val extra = repo.newEditionNow().getOrThrow()

        assertEquals("Extra opening.", extra.briefing)
        assertEquals(EditionRole.extra, extra.role)
        assertEquals(1, calls)
        assertEquals("Morning stays", repo.loadCurrentMorningEdition().getOrThrow().briefing)
    }

    @Test
    fun sampleRetry_withKey_staysExtractive() = runBlocking {
        var calls = 0
        val store = FakeStore(settings = ReaderSettings(geminiKey = "configured"))
        val repo = LocalEditionRepository(
            store,
            today = { sampleDay },
            summarize = { _, _ ->
                calls += 1
                "Should not be stored."
            },
        )

        val retried = repo.retrySample().getOrThrow()

        assertTrue(retried.isSample)
        assertTrue(retried.briefing.endsWith("Summary assembled from headlines."))
        assertEquals(0, calls)
    }

    @Test
    fun saveGeminiKey_replacesASavedKeyAndBlankClearsIt() {
        val store = FakeStore(settings = ReaderSettings(geminiKey = "configured"))
        val repo = LocalEditionRepository(store)

        assertEquals("configured", repo.savedGeminiKey())
        assertTrue(repo.saveGeminiKey("  next  ").isSuccess)
        assertEquals("next", repo.savedGeminiKey())
        assertTrue(repo.saveGeminiKey("   ").isSuccess)
        assertEquals("", repo.savedGeminiKey())
    }

    @Test
    fun saveGeminiKey_failedWriteKeepsThePreviousKey() {
        val store = FakeStore(settings = ReaderSettings(geminiKey = "configured"))
        store.failSettingsWrite = true
        val repo = LocalEditionRepository(store)

        assertTrue(repo.saveGeminiKey("next").isFailure)
        assertEquals("configured", store.readSettings()?.geminiKey)
    }
}

class SampleGeneratorTest {
    @Test
    fun bundledFeeds_keepSourceOrderAndCalendarTimes() {
        val edition = SampleGenerator.forDay(LocalDate(2026, 11, 8))
        assertEquals(
            listOf(
                "Central banks signal a longer pause on rates",
                "Tech firms outline cautious hiring plans",
                "Brief: Transit upgrades set for weekend",
            ),
            edition.stories.map { it.headline },
        )
        assertEquals(
            listOf(StoryRank.lead, StoryRank.secondary, StoryRank.note),
            edition.stories.map { it.rank },
        )
        assertEquals(listOf("Sample Wire", "Sample Herald", "City Desk"), edition.stories.map { it.sourceName })
        assertEquals(
            listOf(
                EditionEvent("Team standup", "9:00"),
                EditionEvent("Lunch with Alex", "12:30"),
            ),
            edition.events,
        )
        assertTrue(edition.tasks.isEmpty())
        assertEquals("2026-11-08", edition.dayKey)
    }

    @Test
    fun emptySourceListDocument_decodesAsPresentAndEmpty() {
        val list = decodeSourceList("{}")
        assertTrue(list.feeds.isEmpty())
        assertTrue(list.calendars.isEmpty())
    }

    @Test
    fun emptySettingsDocument_defaultsTopicsToEmpty() {
        val settings = decodeReaderSettings("{}")
        assertTrue(settings.topics.isEmpty())
        assertTrue(encodeReaderSettings(ReaderSettings()).contains("\"topics\":[]"))
        assertEquals(DEFAULT_NOTIFICATION_TIME, settings.notificationTime)
    }

    @Test
    fun saveNotificationTime_normalizesLeadingZeros() {
        val store = FakeStore()
        val repo = LocalEditionRepository(store)
        assertTrue(repo.saveNotificationTime("7:5").isSuccess)
        assertEquals("07:05", store.readSettings()?.notificationTime)
    }

    @Test
    fun saveTopics_preservesNotificationTime() {
        val store = FakeStore(settings = ReaderSettings(notificationTime = "08:30"))
        val repo = LocalEditionRepository(store)
        repo.saveTopics(listOf("Rust"))
        assertEquals("08:30", store.readSettings()?.notificationTime)
        assertEquals(listOf("Rust"), store.readSettings()?.topics)
    }

    @Test
    fun savedNotificationTime_defaultsToSevenWhenSettingsAbsent() {
        val repo = LocalEditionRepository(FakeStore())
        assertEquals(DEFAULT_NOTIFICATION_TIME, repo.savedNotificationTime())
    }

    @Test
    fun saveNotificationBody_persistsTrimmedSentence() {
        val store = FakeStore()
        val repo = LocalEditionRepository(store)
        assertTrue(repo.saveNotificationBody("  Morning. ").isSuccess)
        assertEquals("Morning.", repo.savedNotificationBody())
    }

    @Test
    fun saveEditionThemeAndLayout_persistKnownIds() {
        val store = FakeStore()
        val repo = LocalEditionRepository(store)
        assertTrue(repo.saveEditionTheme(DEFAULT_EDITION_THEME).isSuccess)
        assertTrue(repo.saveEditionLayout(DEFAULT_EDITION_LAYOUT).isSuccess)
        assertEquals(DEFAULT_EDITION_THEME, repo.savedEditionTheme())
        assertEquals(DEFAULT_EDITION_LAYOUT, repo.savedEditionLayout())
    }
}
