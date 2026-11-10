package hu.toliver.matutine.edition

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditionGenerateStateTest {
    private val day = LocalDate(2026, 11, 8)
    private val sample = SampleGenerator.forDay(day)
    private val finished = sample.copy(id = "morning-2026-11-08", revision = 1, isSample = false)

    @Test
    fun generateKeepsTheVisibleEditionUntilTheNewOneIsReady() {
        val gate = CompletableDeferred<Edition>()
        val repository = FakeEditionRepository(sample) { gate.await() }
        val viewModel = viewModel(repository)

        val before = viewModel.state as EditionUiState.Ready
        assertTrue(before.edition.isSample)
        viewModel.generate()
        viewModel.generate()
        assertEquals(1, repository.generateCalls)

        val generating = viewModel.state as EditionUiState.Ready
        assertTrue(generating.generating)
        assertEquals(sample.mastheadName, generating.edition.mastheadName)
        assertEquals(sample.dateLine, generating.edition.dateLine)
        assertTrue(generating.edition.isSample)
        assertEquals(null, generating.notice)

        gate.complete(finished)

        val ready = viewModel.state as EditionUiState.Ready
        assertFalse(ready.generating)
        assertFalse(ready.edition.isSample)
        assertEquals(finished.id, ready.edition.id)
        assertEquals(null, ready.notice)
        assertEquals(null, viewModel.feedFieldError)
    }

    @Test
    fun generateSuccessSavesTheNewUrlAndClearsTheFieldError() {
        val repository = FakeEditionRepository(sample, generate = { finished })
        val viewModel = viewModel(repository)
        viewModel.updateFeedUrl("  https://example.com/rss.xml  ")
        viewModel.saveFeed()
        assertEquals(FEED_DIDNT_LOAD, viewModel.feedFieldError)
        repository.saveSucceeds = true

        viewModel.generate()

        val ready = viewModel.state as EditionUiState.Ready
        assertFalse(ready.generating)
        assertEquals(finished.id, ready.edition.id)
        assertFalse(ready.edition.isSample)
        assertEquals(null, viewModel.feedFieldError)
        assertEquals("https://example.com/rss.xml", viewModel.feedUrl)
        assertEquals(listOf("https://example.com/rss.xml"), repository.savedFeedUrls())
        assertEquals(1, repository.generateCalls)
    }

    @Test
    fun failedGenerateOfSavedUrlKeepsTheSample() {
        val repository = FakeEditionRepository(
            sample,
            initialFeeds = listOf("https://example.com/saved.xml"),
            generate = { error(FEED_DIDNT_LOAD) },
        )
        val viewModel = viewModel(repository)

        viewModel.generate()

        val ready = viewModel.state as EditionUiState.Ready
        assertFalse(ready.generating)
        assertTrue(ready.edition.isSample)
        assertEquals(FEED_DIDNT_LOAD, ready.notice)
        assertEquals("https://example.com/saved.xml", viewModel.feedUrl)
        assertEquals(1, repository.generateCalls)
        assertEquals(0, repository.saveCalls)
    }

    @Test
    fun failedNewUrlDoesNotGenerate() {
        val repository = FakeEditionRepository(sample, generate = { finished })
        val viewModel = viewModel(repository)
        viewModel.updateFeedUrl("https://example.com/broken.xml")

        viewModel.generate()

        val ready = viewModel.state as EditionUiState.Ready
        assertFalse(ready.generating)
        assertTrue(ready.edition.isSample)
        assertEquals(FEED_DIDNT_LOAD, ready.notice)
        assertEquals("https://example.com/broken.xml", viewModel.feedUrl)
        assertEquals(FEED_DIDNT_LOAD, viewModel.feedFieldError)
        assertEquals(0, repository.generateCalls)
        assertEquals(emptyList(), repository.savedFeedUrls())
    }

    @Test
    fun initialFieldIsTheSavedUrl() {
        val repository = FakeEditionRepository(sample, initialFeeds = listOf("https://example.com/saved.xml"))
        val viewModel = viewModel(repository)

        assertEquals("https://example.com/saved.xml", viewModel.feedUrl)
        assertEquals(null, viewModel.feedFieldError)
        assertTrue(viewModel.state is EditionUiState.Ready)
    }

    @Test
    fun corruptSourceListLeavesTheFieldEmptyAndStillLoads() {
        val repository = FakeEditionRepository(sample, failSavedFeedUrls = true)
        val viewModel = viewModel(repository)

        assertEquals("", viewModel.feedUrl)
        assertTrue(viewModel.state is EditionUiState.Ready)
    }

    @Test
    fun saveFeedSuccessStoresTheTrimmedUrl() {
        val repository = FakeEditionRepository(sample, initialSaveSucceeds = true)
        val viewModel = viewModel(repository)
        viewModel.updateFeedUrl("  https://example.com/rss.xml  ")

        viewModel.saveFeed()

        assertEquals("https://example.com/rss.xml", viewModel.feedUrl)
        assertEquals(null, viewModel.feedFieldError)
        assertEquals(listOf("https://example.com/rss.xml"), repository.savedFeedUrls())
    }

    @Test
    fun saveFeedFailureKeepsTheTypedUrl() {
        val repository = FakeEditionRepository(sample)
        val viewModel = viewModel(repository)
        viewModel.updateFeedUrl("https://example.com/broken.xml")

        viewModel.saveFeed()

        assertEquals("https://example.com/broken.xml", viewModel.feedUrl)
        assertEquals(FEED_DIDNT_LOAD, viewModel.feedFieldError)
        assertEquals(emptyList(), repository.savedFeedUrls())

        viewModel.updateFeedUrl("https://example.com/next.xml")
        assertEquals("https://example.com/next.xml", viewModel.feedUrl)
        assertEquals(null, viewModel.feedFieldError)
    }

    @Test
    fun saveFeedDoesNotReplaceTextTypedWhileSaving() {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeEditionRepository(sample, initialSaveSucceeds = true, blockSave = gate)
        val viewModel = viewModel(repository)
        viewModel.updateFeedUrl("https://example.com/first.xml")
        viewModel.saveFeed()
        viewModel.updateFeedUrl("https://example.com/second.xml")

        gate.complete(Unit)

        assertEquals("https://example.com/second.xml", viewModel.feedUrl)
        assertEquals(null, viewModel.feedFieldError)
        assertEquals(listOf("https://example.com/first.xml"), repository.savedFeedUrls())
    }

    @Test
    fun topicsLoadFromTheRepositoryAndSaveReadsThemBack() {
        val repository = FakeEditionRepository(
            sample,
            initialTopics = listOf("Technology", "Video games"),
        )
        val viewModel = viewModel(repository)
        assertEquals(listOf("Technology", "Video games"), viewModel.topics)

        assertTrue(viewModel.saveTopics(listOf("Rust", "school")))
        assertEquals(listOf("Rust", "school"), viewModel.topics)
    }

    @Test
    fun savedTopicsFailureLeavesTheListEmpty() {
        val repository = FakeEditionRepository(sample, failSavedTopics = true)
        val viewModel = viewModel(repository)

        assertEquals(emptyList(), viewModel.topics)
        assertTrue(viewModel.state is EditionUiState.Ready)
    }

    @Test
    fun failedCalendarSaveStillGeneratesFromTheStoredUrl() {
        val repository = FakeEditionRepository(
            sample,
            initialCalendars = listOf("https://example.com/old.ics"),
            generate = { finished },
        )
        val viewModel = viewModel(repository)
        viewModel.updateCalendarUrl("https://example.com/closed.ics")

        viewModel.generate()

        val ready = viewModel.state as EditionUiState.Ready
        assertFalse(ready.generating)
        assertEquals(finished.id, ready.edition.id)
        assertEquals(CALENDAR_LINK_DIDNT_OPEN, ready.notice)
        assertEquals(CALENDAR_LINK_DIDNT_OPEN, viewModel.calendarFieldError)
        assertEquals(listOf("https://example.com/old.ics"), repository.savedCalendarUrls())
        assertEquals(1, repository.generateCalls)
    }

    @Test
    fun failedCalendarSaveAndFailedGenerateShowsTheFeedSentence() {
        val repository = FakeEditionRepository(sample, generate = { error(FEED_DIDNT_LOAD) })
        val viewModel = viewModel(repository)
        viewModel.updateCalendarUrl("https://example.com/closed.ics")

        viewModel.generate()

        val ready = viewModel.state as EditionUiState.Ready
        assertEquals(FEED_DIDNT_LOAD, ready.notice)
        assertEquals(CALENDAR_LINK_DIDNT_OPEN, viewModel.calendarFieldError)
        assertEquals(emptyList(), repository.savedCalendarUrls())
    }

    @Test
    fun saveCalendarFailureKeepsTheTypedUrl() {
        val repository = FakeEditionRepository(sample)
        val viewModel = viewModel(repository)
        viewModel.updateCalendarUrl("https://example.com/closed.ics")

        viewModel.saveCalendar()

        assertEquals("https://example.com/closed.ics", viewModel.calendarUrl)
        assertEquals(CALENDAR_LINK_DIDNT_OPEN, viewModel.calendarFieldError)
        assertEquals(emptyList(), repository.savedCalendarUrls())
        assertEquals(1, repository.saveCalendarCalls)
    }

    @Test
    fun saveCalendarSuccessStoresTheTrimmedUrlAndGenerateDoesNotSaveAgain() {
        val repository = FakeEditionRepository(sample, generate = { finished })
        repository.saveCalendarSucceeds = true
        val viewModel = viewModel(repository)
        viewModel.updateCalendarUrl("  https://example.com/cal.ics  ")

        viewModel.saveCalendar()

        assertEquals("https://example.com/cal.ics", viewModel.calendarUrl)
        assertEquals(null, viewModel.calendarFieldError)
        assertEquals(listOf("https://example.com/cal.ics"), repository.savedCalendarUrls())
        assertEquals(1, repository.saveCalendarCalls)

        viewModel.generate()

        assertEquals(1, repository.saveCalendarCalls)
        assertEquals(1, repository.generateCalls)
        assertEquals(null, (viewModel.state as EditionUiState.Ready).notice)
    }

    @Test
    fun saveCalDavSuccessClearsThePasswordAndGenerateDoesNotSaveAgain() {
        val repository = FakeEditionRepository(sample, generate = { finished })
        repository.saveCalDavSucceeds = true
        val viewModel = viewModel(repository)
        viewModel.updateCalDavUrl("  https://calendar.example/dav/personal/  ")
        viewModel.updateCalDavUsername(" o ")
        viewModel.updateCalDavPassword("local-secret")

        viewModel.saveCalDav()

        assertEquals("https://calendar.example/dav/personal/", viewModel.calDavUrl)
        assertEquals("o", viewModel.calDavUsername)
        assertEquals("", viewModel.calDavPassword)
        assertEquals(null, viewModel.calDavFieldError)
        assertEquals("local-secret", repository.savedCalDavPassword)
        assertEquals(1, repository.saveCalDavCalls)

        viewModel.generate()

        assertEquals(1, repository.saveCalDavCalls)
        assertEquals(1, repository.generateCalls)
    }

    @Test
    fun storedCalendarAndCalDavAppearWithABlankPassword() {
        val repository = FakeEditionRepository(
            sample,
            initialCalendars = listOf("https://example.com/cal.ics"),
            initialCalDav = CalDavAccount(
                url = "https://calendar.example/dav/personal/",
                username = "o",
                password = "local-secret",
            ),
        )
        val viewModel = viewModel(repository)

        assertEquals("https://example.com/cal.ics", viewModel.calendarUrl)
        assertEquals("https://calendar.example/dav/personal/", viewModel.calDavUrl)
        assertEquals("o", viewModel.calDavUsername)
        assertEquals("", viewModel.calDavPassword)
    }

    @Test
    fun missingMorningIsAnEmptyPageAndGenerateFillsIt() {
        val gate = CompletableDeferred<Edition>()
        val repository = FakeEditionRepository(
            finished,
            missingMorning = true,
            generate = { gate.await() },
        )
        val viewModel = viewModel(repository)

        val empty = viewModel.state as EditionUiState.MissingMorning
        assertFalse(empty.generating)
        assertEquals(null, empty.notice)
        viewModel.generate()
        viewModel.generate()
        assertEquals(1, repository.generateCalls)

        val generating = viewModel.state as EditionUiState.MissingMorning
        assertTrue(generating.generating)
        assertEquals(null, generating.notice)

        gate.complete(finished)

        val ready = viewModel.state as EditionUiState.Ready
        assertFalse(ready.generating)
        assertEquals(finished.id, ready.edition.id)
        assertEquals(null, ready.notice)
        assertFalse(ready.edition.stories.isEmpty())
    }

    @Test
    fun failedGenerateFromMissingMorningStaysEmptyWithTheFeedSentence() {
        val repository = FakeEditionRepository(
            finished,
            missingMorning = true,
            generate = { error(FEED_DIDNT_LOAD) },
        )
        val viewModel = viewModel(repository)

        viewModel.generate()

        val empty = viewModel.state as EditionUiState.MissingMorning
        assertFalse(empty.generating)
        assertEquals(FEED_DIDNT_LOAD, empty.notice)
        assertEquals(1, repository.generateCalls)
    }

    @Test
    fun otherErrorsStillDoNotGenerate() {
        val repository = FakeEditionRepository(
            finished,
            loadError = "Couldn't load the edition.",
            generate = { finished },
        )
        val viewModel = viewModel(repository)

        assertEquals("Couldn't load the edition.", (viewModel.state as EditionUiState.OtherError).message)
        viewModel.generate()
        assertEquals(0, repository.generateCalls)
        assertTrue(viewModel.state is EditionUiState.OtherError)
    }

    @Test
    fun failedCalDavSaveStillGenerates() {
        val repository = FakeEditionRepository(sample, generate = { finished })
        val viewModel = viewModel(repository)
        viewModel.updateCalDavUrl("https://calendar.example/dav/personal/")
        viewModel.updateCalDavUsername("o")
        viewModel.updateCalDavPassword("local-secret")

        viewModel.generate()

        val ready = viewModel.state as EditionUiState.Ready
        assertEquals(finished.id, ready.edition.id)
        assertEquals(CALDAV_DIDNT_LOAD, ready.notice)
        assertEquals(CALDAV_DIDNT_LOAD, viewModel.calDavFieldError)
        assertEquals("local-secret", viewModel.calDavPassword)
        assertEquals(1, repository.generateCalls)
    }

    @Test
    fun newEditionNowLeavesTheMorningOnTheFrontPageAndTheListOpensTheExtra() {
        val repository = FakeEditionRepository(finished)
        val viewModel = viewModel(repository)

        viewModel.newEditionNow()

        val front = viewModel.state as EditionUiState.Ready
        assertEquals(finished.id, front.edition.id)
        assertEquals(EditionRole.morning, front.edition.role)
        assertEquals(finished.briefing, front.edition.briefing)
        val extra = front.sameDayExtras.single()
        assertEquals(EditionRole.extra, extra.role)
        assertEquals(extra.dayKey, finished.dayKey)
        assertEquals(sameDayExtraLabel(extra), extra.stories.first().headline)

        viewModel.openExtra(extra.id)
        val opened = viewModel.state as EditionUiState.Ready
        assertEquals(extra.id, opened.edition.id)
        assertEquals(listOf(extra), opened.sameDayExtras)

        viewModel.openMorning()
        val back = viewModel.state as EditionUiState.Ready
        assertEquals(finished.id, back.edition.id)
        assertEquals(EditionRole.morning, back.edition.role)
        assertTrue(back.sameDayExtras.all { it.dayKey == finished.dayKey })
    }

    @Test
    fun failedNewEditionNowKeepsTheVisiblePaper() {
        val repository = FakeEditionRepository(finished)
        repository.failNewEdition = true
        val viewModel = viewModel(repository)

        viewModel.newEditionNow()

        val ready = viewModel.state as EditionUiState.Ready
        assertFalse(ready.generating)
        assertEquals(finished.id, ready.edition.id)
        assertEquals(FEED_DIDNT_LOAD, ready.notice)
        assertTrue(ready.sameDayExtras.isEmpty())
    }

    @Test
    fun generatePersistsADirtyGeminiKeyBeforeGenerating() {
        val repository = FakeEditionRepository(sample, generate = { finished })
        val viewModel = viewModel(repository)
        viewModel.updateGeminiKey("  configured  ")

        viewModel.generate()

        assertEquals("configured", viewModel.geminiKey)
        assertEquals("configured", repository.savedGeminiKey())
        assertEquals(1, repository.generateCalls)
    }

    @Test
    fun failedGeminiKeySaveKeepsThePreviousKeyAndStillGenerates() {
        val repository = FakeEditionRepository(sample, generate = { finished })
        repository.geminiKey = "configured"
        val viewModel = viewModel(repository)
        viewModel.updateGeminiKey("next")
        repository.failGeminiSave = true

        viewModel.generate()

        assertEquals("configured", viewModel.geminiKey)
        assertEquals("configured", repository.savedGeminiKey())
        assertEquals(1, repository.generateCalls)
    }

    private fun viewModel(repository: EditionRepository) =
        EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))
}

private class FakeEditionRepository(
    private val edition: Edition,
    initialFeeds: List<String> = emptyList(),
    initialCalendars: List<String> = emptyList(),
    initialTopics: List<String> = emptyList(),
    initialCalDav: CalDavAccount? = null,
    initialSaveSucceeds: Boolean = false,
    private val failSavedFeedUrls: Boolean = false,
    private val failSavedTopics: Boolean = false,
    private val missingMorning: Boolean = false,
    private val loadError: String? = null,
    var blockSave: CompletableDeferred<Unit>? = null,
    private val generate: suspend () -> Edition = { edition },
) : EditionRepository {
    private val feeds = initialFeeds.toMutableList()
    private val calendars = initialCalendars.toMutableList()
    private val topics = initialTopics.toMutableList()
    private val calDavAccount = initialCalDav
    var saveSucceeds: Boolean = initialSaveSucceeds
    var saveCalendarSucceeds: Boolean = false
    var saveCalDavSucceeds: Boolean = false
    var generateCalls: Int = 0
    var saveCalls: Int = 0
    var saveCalendarCalls: Int = 0
    var saveCalDavCalls: Int = 0
    var savedCalDavPassword: String = ""

    override suspend fun loadCurrentMorningEdition(): Result<Edition> = when {
        missingMorning -> Result.failure(IllegalStateException(MORNING_EDITION_NOT_READY))
        loadError != null -> Result.failure(IllegalStateException(loadError))
        else -> Result.success(edition)
    }

    override suspend fun retrySample(): Result<Edition> = Result.success(edition)

    override fun savedFeedUrls(): List<String> {
        if (failSavedFeedUrls) error("corrupt sources")
        return feeds.toList()
    }

    override fun savedTopics(): List<String> {
        if (failSavedTopics) error("corrupt settings")
        return topics.toList()
    }

    override fun saveTopics(topics: List<String>) {
        this.topics.clear()
        this.topics += topics
    }

    override fun savedNotificationTime(): String = DEFAULT_NOTIFICATION_TIME

    override fun saveNotificationTime(time: String): Result<Unit> = Result.success(Unit)

    override fun savedNotificationBody(): String = EDITION_READY_NOTIFICATION_BODY

    override fun saveNotificationBody(body: String): Result<Unit> = Result.success(Unit)

    override fun savedEditionTheme(): String = DEFAULT_EDITION_THEME

    override fun saveEditionTheme(themeId: String): Result<Unit> = Result.success(Unit)

    override fun savedEditionLayout(): String = DEFAULT_EDITION_LAYOUT

    override fun saveEditionLayout(layoutId: String): Result<Unit> = Result.success(Unit)

    var geminiKey: String = ""
    var failGeminiSave: Boolean = false

    override fun savedGeminiKey(): String = geminiKey

    override fun saveGeminiKey(key: String): Result<Unit> {
        if (failGeminiSave) return Result.failure(IllegalStateException("settings_unavailable"))
        geminiKey = key.trim()
        return Result.success(Unit)
    }

    override suspend fun saveFeedUrl(url: String): Result<Unit> {
        saveCalls += 1
        blockSave?.await()
        if (!saveSucceeds) return Result.failure(IllegalStateException(FEED_DIDNT_LOAD))
        feeds.clear()
        feeds += url.trim()
        return Result.success(Unit)
    }

    override fun savedCalendarUrls(): List<String> = calendars.toList()

    override suspend fun saveCalendarUrl(url: String): Result<Unit> {
        saveCalendarCalls += 1
        if (!saveCalendarSucceeds) return Result.failure(IllegalStateException(CALENDAR_LINK_DIDNT_OPEN))
        calendars.clear()
        calendars += url.trim()
        return Result.success(Unit)
    }

    override fun savedCalDavAccount(): CalDavAccount? = calDavAccount?.copy(password = "")

    override suspend fun saveCalDavAccount(url: String, username: String, password: String): Result<Unit> {
        saveCalDavCalls += 1
        if (!saveCalDavSucceeds) return Result.failure(IllegalStateException(CALDAV_DIDNT_LOAD))
        savedCalDavPassword = password
        return Result.success(Unit)
    }

    override suspend fun generateToday(): Result<Edition> {
        generateCalls += 1
        return runCatching { generate() }
    }

    private val extras = mutableListOf<Edition>()
    var failNewEdition: Boolean = false

    override suspend fun newEditionNow(): Result<Edition> {
        if (failNewEdition) return Result.failure(IllegalStateException(FEED_DIDNT_LOAD))
        val extra = edition.copy(
            id = "extra-${edition.dayKey}-1",
            revision = 1,
            role = EditionRole.extra,
            briefing = "Fresh extra",
        )
        extras += extra
        return Result.success(extra)
    }

    override suspend fun loadSameDayExtras(): List<Edition> = extras.toList()

    override suspend fun updateMorningEdition(edition: Edition): Result<Edition> = Result.success(edition)

    override suspend fun updateExtra(edition: Edition): Result<Edition> =
        Result.success(edition.copy(revision = edition.revision + 1))
}
