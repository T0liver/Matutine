package hu.toliver.matutine.edition

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeStore(
    private var morning: Edition? = null,
    private var sourceList: SourceList? = null,
    private var settings: ReaderSettings? = null,
) : EditionStore {
    var saved: Edition? = null

    override fun readSourceList(): SourceList? = sourceList

    override fun writeSourceList(sourceList: SourceList) {
        this.sourceList = sourceList
    }

    override fun readSettings(): ReaderSettings? = settings

    override fun writeSettings(settings: ReaderSettings) {
        this.settings = settings
    }

    override fun loadMorningEdition(dayKey: String): Edition? =
        morning?.takeIf { it.dayKey == dayKey && it.role == EditionRole.morning }

    override fun saveMorningEdition(edition: Edition) {
        morning = edition
        saved = edition
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
            assertTrue(repo.loadCurrentMorningEdition().isFailure)
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
    }
}
