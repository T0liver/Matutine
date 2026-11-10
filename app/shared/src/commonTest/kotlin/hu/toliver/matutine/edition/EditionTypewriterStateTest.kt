package hu.toliver.matutine.edition

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditionTypewriterStateTest {
    private val edition = Edition(
        id = "morning-2026-11-10",
        revision = 2,
        dayKey = "2026-11-10",
        role = EditionRole.morning,
        mastheadName = "Matutine",
        dateLine = "Tuesday, November 10, 2026",
        isSample = false,
        briefing = "The harbor is open.",
        stories = listOf(
            EditionStory("Lead", "Desk", "Deck.", "https://example.com/lead", StoryRank.lead),
        ),
        events = listOf(EditionEvent("Standup", "09:00")),
        tasks = listOf(EditionTask("Milk")),
    )

    @Test
    fun saveRewritesTheEditionAndKeepsTheDayKey() {
        val repository = TrackingEditionRepository(edition)
        val viewModel = EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))
        val text = EditionTranscript.render(edition).replace(edition.dateLine, "Wednesday, November 11, 2026")
            .replace(edition.briefing, "Rewritten briefing.")

        viewModel.saveTypewriter(text)

        val ready = viewModel.state as EditionUiState.Ready
        assertEquals("Rewritten briefing.", ready.edition.briefing)
        assertEquals("Wednesday, November 11, 2026", ready.edition.dateLine)
        assertEquals(edition.dayKey, ready.edition.dayKey)
        assertEquals(edition.id, ready.edition.id)
        assertTrue(viewModel.typewriterSaved)
        assertFalse(ready.taskSaved)
        assertNull(viewModel.typewriterError)
        assertEquals(1, repository.updateCalls)
    }

    @Test
    fun failedSaveKeepsTheTextSignalAndThePriorSavedLine() {
        val repository = TrackingEditionRepository(edition)
        val viewModel = EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))
        viewModel.saveTypewriter(rewritten("First save."))
        repository.fail = true

        viewModel.saveTypewriter(rewritten("Second save."))

        val ready = viewModel.state as EditionUiState.Ready
        assertEquals("First save.", ready.edition.briefing)
        assertTrue(viewModel.typewriterSaved)
        assertEquals(EditionTranscript.SAVE_FAILED, viewModel.typewriterError)
    }

    @Test
    fun saveInFlightLeavesSavedInPlace() {
        val repository = TrackingEditionRepository(edition)
        val viewModel = EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))
        viewModel.saveTypewriter(rewritten("First save."))
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate

        viewModel.saveTypewriter(rewritten("Still writing."))

        assertTrue(viewModel.typewriterSaved)
        assertNull(viewModel.typewriterError)
        gate.complete(Unit)
        assertEquals("Still writing.", (viewModel.state as EditionUiState.Ready).edition.briefing)
        assertTrue(viewModel.typewriterSaved)
        assertNull(viewModel.typewriterError)
    }

    @Test
    fun discardRestoresTheLastSavedEdition() {
        val repository = TrackingEditionRepository(edition)
        val viewModel = EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))
        repository.fail = true
        viewModel.saveTypewriter(rewritten("Thrown away."))

        val restored = viewModel.discardTypewriter()

        assertEquals(EditionTranscript.render(edition), restored)
        assertNull(viewModel.typewriterError)
        assertEquals(edition.briefing, (viewModel.state as EditionUiState.Ready).edition.briefing)
    }

    @Test
    fun escapeClosesAfterASuccessfulSave() {
        val repository = TrackingEditionRepository(edition)
        val viewModel = EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))
        var closed = false

        viewModel.escapeTypewriter(rewritten("Closed briefing.")) { closed = true }

        assertTrue(closed)
        assertEquals("Closed briefing.", (viewModel.state as EditionUiState.Ready).edition.briefing)
        assertEquals(
            listOf(StoryRank.lead),
            (viewModel.state as EditionUiState.Ready).edition.stories.map { it.rank },
        )
    }

    @Test
    fun escapeStaysOpenWhenTheSaveFails() {
        val repository = TrackingEditionRepository(edition)
        val viewModel = EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))
        repository.fail = true
        var closed = false

        viewModel.escapeTypewriter(rewritten("Still here.")) { closed = true }

        assertFalse(closed)
        assertEquals(EditionTranscript.SAVE_FAILED, viewModel.typewriterError)
        assertEquals(edition.briefing, (viewModel.state as EditionUiState.Ready).edition.briefing)
    }

    private fun rewritten(briefing: String): String =
        EditionTranscript.render(edition).replace(edition.briefing, briefing)

    private class TrackingEditionRepository(
        private var morning: Edition,
    ) : EditionRepository {
        var updateCalls: Int = 0
        var fail: Boolean = false
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun loadCurrentMorningEdition(): Result<Edition> = Result.success(morning)

        override suspend fun retrySample(): Result<Edition> = Result.success(morning)

        override fun savedFeedUrls(): List<String> = emptyList()

        override suspend fun saveFeedUrl(url: String): Result<Unit> = Result.success(Unit)

        override fun savedCalendarUrls(): List<String> = emptyList()

        override suspend fun saveCalendarUrl(url: String): Result<Unit> = Result.success(Unit)

        override fun savedCalDavAccount(): CalDavAccount? = null

        override suspend fun saveCalDavAccount(url: String, username: String, password: String): Result<Unit> =
            Result.success(Unit)

        override fun savedTopics(): List<String> = emptyList()

        override fun saveTopics(topics: List<String>) = Unit

        override fun savedNotificationTime(): String = DEFAULT_NOTIFICATION_TIME

        override fun saveNotificationTime(time: String): Result<Unit> = Result.success(Unit)

        override fun savedNotificationBody(): String = EDITION_READY_NOTIFICATION_BODY

        override fun saveNotificationBody(body: String): Result<Unit> = Result.success(Unit)

        override fun savedEditionTheme(): String = DEFAULT_EDITION_THEME

        override fun saveEditionTheme(themeId: String): Result<Unit> = Result.success(Unit)

        override fun savedEditionLayout(): String = DEFAULT_EDITION_LAYOUT

        override fun saveEditionLayout(layoutId: String): Result<Unit> = Result.success(Unit)

        override fun savedGeminiKey(): String = ""

        override fun saveGeminiKey(key: String): Result<Unit> = Result.success(Unit)

        override suspend fun generateToday(): Result<Edition> = Result.success(morning)

        override suspend fun newEditionNow(): Result<Edition> = Result.success(morning)

        override suspend fun loadSameDayExtras(): List<Edition> = emptyList()

        override suspend fun updateMorningEdition(edition: Edition): Result<Edition> {
            gate?.await()
            if (fail) return Result.failure(IllegalStateException("save failed"))
            updateCalls += 1
            val next = edition.copy(revision = morning.revision + 1)
            morning = next
            return Result.success(next)
        }

        override suspend fun updateExtra(edition: Edition): Result<Edition> = Result.success(edition)
    }
}
