package hu.toliver.matutine.edition

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditionTaskStateTest {
    private val day = LocalDate(2026, 11, 10)
    private val edition = SampleGenerator.forDay(day).copy(
        id = "morning-2026-11-10",
        revision = 2,
        isSample = false,
        tasks = listOf(EditionTask("Buy milk")),
    )

    @Test
    fun toggleTaskMarksCompleteAndSaves() {
        val repository = TrackingEditionRepository(edition)
        val viewModel = EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))

        viewModel.toggleTask(0)

        val ready = viewModel.state as EditionUiState.Ready
        assertTrue(ready.edition.tasks.single().completed)
        assertTrue(ready.taskSaved)
        assertEquals(1, repository.updateCalls)
        assertTrue(repository.lastSaved?.tasks?.single()?.completed == true)
        assertEquals(3, repository.lastSaved?.revision)
    }

    @Test
    fun addTaskAppendsAndSaves() {
        val repository = TrackingEditionRepository(edition)
        val viewModel = EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))

        viewModel.addTask("  Walk the dog  ")

        val ready = viewModel.state as EditionUiState.Ready
        assertEquals(2, ready.edition.tasks.size)
        assertEquals("Walk the dog", ready.edition.tasks.last().title)
        assertFalse(ready.edition.tasks.last().completed)
        assertTrue(ready.taskSaved)
        assertEquals(1, repository.updateCalls)
    }

    @Test
    fun blankAddIsIgnored() {
        val repository = TrackingEditionRepository(edition)
        val viewModel = EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))

        viewModel.addTask("   ")

        val ready = viewModel.state as EditionUiState.Ready
        assertEquals(1, ready.edition.tasks.size)
        assertFalse(ready.taskSaved)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun reloadKeepsSavedTasks() {
        val stored = edition.copy(
            tasks = listOf(
                EditionTask("Buy milk", completed = true),
                EditionTask("Walk the dog"),
            ),
            revision = 4,
        )
        val repository = TrackingEditionRepository(stored)
        val viewModel = EditionViewModel(repository, CoroutineScope(Dispatchers.Unconfined))

        val ready = viewModel.state as EditionUiState.Ready
        assertTrue(ready.edition.tasks.first().completed)
        assertEquals("Walk the dog", ready.edition.tasks.last().title)
    }

    private class TrackingEditionRepository(
        private var morning: Edition,
    ) : EditionRepository {
        var updateCalls: Int = 0
        var lastSaved: Edition? = null

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

        override suspend fun generateToday(): Result<Edition> = Result.success(morning)

        override suspend fun updateMorningEdition(edition: Edition): Result<Edition> {
            updateCalls += 1
            val next = edition.copy(revision = morning.revision + 1)
            morning = next
            lastSaved = next
            return Result.success(next)
        }
    }
}
