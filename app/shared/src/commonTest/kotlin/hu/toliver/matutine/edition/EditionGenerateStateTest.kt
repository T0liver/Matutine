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

    private fun viewModel(repository: EditionRepository) =
        EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))
}

private class FakeEditionRepository(
    private val edition: Edition,
    initialFeeds: List<String> = emptyList(),
    initialTopics: List<String> = emptyList(),
    initialSaveSucceeds: Boolean = false,
    private val failSavedFeedUrls: Boolean = false,
    private val failSavedTopics: Boolean = false,
    var blockSave: CompletableDeferred<Unit>? = null,
    private val generate: suspend () -> Edition = { edition },
) : EditionRepository {
    private val feeds = initialFeeds.toMutableList()
    private val topics = initialTopics.toMutableList()
    var saveSucceeds: Boolean = initialSaveSucceeds
    var generateCalls: Int = 0
    var saveCalls: Int = 0

    override suspend fun loadCurrentMorningEdition(): Result<Edition> = Result.success(edition)

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

    override suspend fun saveFeedUrl(url: String): Result<Unit> {
        saveCalls += 1
        blockSave?.await()
        if (!saveSucceeds) return Result.failure(IllegalStateException(FEED_DIDNT_LOAD))
        feeds.clear()
        feeds += url.trim()
        return Result.success(Unit)
    }

    override suspend fun generateToday(): Result<Edition> {
        generateCalls += 1
        return runCatching { generate() }
    }
}
