package hu.toliver.matutine.edition

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import hu.toliver.matutine.settings.onNotificationTimeSaved
import kotlinx.coroutines.launch

sealed interface EditionUiState {
    data object Loading : EditionUiState

    data class Ready(
        val edition: Edition,
        val generating: Boolean = false,
        val notice: String? = null,
        val taskSaved: Boolean = false,
        val sameDayExtras: List<Edition> = emptyList(),
    ) : EditionUiState

    data class SampleLoadFailed(val message: String = "The sample edition didn't load.") : EditionUiState

    data class MissingMorning(
        val generating: Boolean = false,
        val notice: String? = null,
    ) : EditionUiState

    data class OtherError(val message: String) : EditionUiState
}

class EditionViewModel(
    private val repository: EditionRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    var state: EditionUiState by mutableStateOf(EditionUiState.Loading)
        private set

    var feedUrl by mutableStateOf("")
        private set

    var feedFieldError by mutableStateOf<String?>(null)
        private set

    var calendarUrl by mutableStateOf("")
        private set

    var calendarFieldError by mutableStateOf<String?>(null)
        private set

    var calDavUrl by mutableStateOf("")
        private set

    var calDavUsername by mutableStateOf("")
        private set

    var calDavPassword by mutableStateOf("")
        private set

    var calDavFieldError by mutableStateOf<String?>(null)
        private set

    var topics by mutableStateOf<List<String>>(emptyList())
        private set

    var notificationTime by mutableStateOf(DEFAULT_NOTIFICATION_TIME)
        private set

    var notificationTimeError by mutableStateOf<String?>(null)
        private set

    var notificationBody by mutableStateOf(EDITION_READY_NOTIFICATION_BODY)
        private set

    var editionTheme by mutableStateOf(DEFAULT_EDITION_THEME)
        private set

    var editionLayout by mutableStateOf(DEFAULT_EDITION_LAYOUT)
        private set

    var geminiKey by mutableStateOf("")
        private set

    var typewriterSaved by mutableStateOf(false)
        private set

    var typewriterError by mutableStateOf<String?>(null)
        private set

    private var typewriterGeneration = 0

    private var feedBaseline: String = ""
    private var calendarBaseline: String = ""
    private var calDavUrlBaseline: String = ""
    private var calDavUsernameBaseline: String = ""
    private var notificationTimeBaseline: String = DEFAULT_NOTIFICATION_TIME
    private var notificationBodyBaseline: String = EDITION_READY_NOTIFICATION_BODY
    private var geminiKeyBaseline: String = ""

    init {
        feedBaseline = runCatching { repository.savedFeedUrls().singleOrNull().orEmpty() }.getOrDefault("")
        feedUrl = feedBaseline
        calendarBaseline = runCatching { repository.savedCalendarUrls().firstOrNull().orEmpty() }.getOrDefault("")
        calendarUrl = calendarBaseline
        val calDav = runCatching { repository.savedCalDavAccount() }.getOrNull()
        calDavUrlBaseline = calDav?.url.orEmpty()
        calDavUsernameBaseline = calDav?.username.orEmpty()
        calDavUrl = calDavUrlBaseline
        calDavUsername = calDavUsernameBaseline
        topics = runCatching { repository.savedTopics() }.getOrDefault(emptyList())
        notificationTimeBaseline = runCatching { repository.savedNotificationTime() }.getOrDefault(DEFAULT_NOTIFICATION_TIME)
        notificationTime = notificationTimeBaseline
        notificationBodyBaseline = runCatching { repository.savedNotificationBody() }.getOrDefault(EDITION_READY_NOTIFICATION_BODY)
        notificationBody = notificationBodyBaseline
        editionTheme = runCatching { repository.savedEditionTheme() }.getOrDefault(DEFAULT_EDITION_THEME)
        editionLayout = runCatching { repository.savedEditionLayout() }.getOrDefault(DEFAULT_EDITION_LAYOUT)
        geminiKeyBaseline = runCatching { repository.savedGeminiKey() }.getOrDefault("")
        geminiKey = geminiKeyBaseline
        load()
    }

    fun updateNotificationTime(value: String) {
        if (value != notificationTime) notificationTimeError = null
        notificationTime = value
    }

    fun saveNotificationTime() {
        val typed = notificationTime
        repository.saveNotificationTime(typed)
            .onSuccess {
                notificationTimeBaseline = repository.savedNotificationTime()
                if (notificationTime == typed) notificationTime = notificationTimeBaseline
                notificationTimeError = null
                onNotificationTimeSaved()
            }
            .onFailure {
                if (notificationTime == typed) notificationTimeError = "Use a time like 07:00."
            }
    }

    fun updateNotificationBody(value: String) {
        notificationBody = value
    }

    fun saveNotificationBody() {
        val typed = notificationBody
        repository.saveNotificationBody(typed)
            .onSuccess {
                notificationBodyBaseline = repository.savedNotificationBody()
                if (notificationBody == typed) notificationBody = notificationBodyBaseline
            }
    }

    fun saveEditionTheme(themeId: String) {
        repository.saveEditionTheme(themeId)
            .onSuccess { editionTheme = repository.savedEditionTheme() }
    }

    fun updateGeminiKey(value: String) {
        geminiKey = value
    }

    fun saveGeminiKey() {
        persistGeminiKey(geminiKey)
    }

    fun saveEditionLayout(layoutId: String) {
        repository.saveEditionLayout(layoutId)
            .onSuccess { editionLayout = repository.savedEditionLayout() }
    }

    fun updateFeedUrl(value: String) {
        if (value != feedUrl) feedFieldError = null
        feedUrl = value
    }

    fun updateCalendarUrl(value: String) {
        if (value != calendarUrl) calendarFieldError = null
        calendarUrl = value
    }

    fun updateCalDavUrl(value: String) {
        if (value != calDavUrl) calDavFieldError = null
        calDavUrl = value
    }

    fun updateCalDavUsername(value: String) {
        if (value != calDavUsername) calDavFieldError = null
        calDavUsername = value
    }

    fun updateCalDavPassword(value: String) {
        if (value != calDavPassword) calDavFieldError = null
        calDavPassword = value
    }

    fun saveFeed() {
        val typed = feedUrl
        scope.launch {
            repository.saveFeedUrl(typed)
                .onSuccess {
                    feedBaseline = typed.trim()
                    if (feedUrl == typed) feedUrl = feedBaseline
                    feedFieldError = null
                }
                .onFailure {
                    if (feedUrl == typed) feedFieldError = FEED_DIDNT_LOAD
                }
        }
    }

    fun saveCalendar() {
        val typed = calendarUrl
        scope.launch {
            repository.saveCalendarUrl(typed)
                .onSuccess {
                    calendarBaseline = typed.trim()
                    if (calendarUrl == typed) calendarUrl = calendarBaseline
                    calendarFieldError = null
                }
                .onFailure {
                    if (calendarUrl == typed) calendarFieldError = CALENDAR_LINK_DIDNT_OPEN
                }
        }
    }

    fun saveCalDav() {
        val typedUrl = calDavUrl
        val typedUsername = calDavUsername
        val typedPassword = calDavPassword
        scope.launch {
            persistCalDav(typedUrl, typedUsername, typedPassword)
        }
    }

    fun saveTopics(values: List<String>): Boolean =
        runCatching {
            repository.saveTopics(values)
            topics = repository.savedTopics()
        }.isSuccess

    fun generate() {
        val restore: (String) -> EditionUiState = when (val current = state) {
            is EditionUiState.Ready -> {
                if (current.generating) return
                val failed = { notice: String -> current.copy(generating = false, notice = notice) }
                state = current.copy(generating = true, notice = null)
                failed
            }
            is EditionUiState.MissingMorning -> {
                if (current.generating) return
                val failed = { notice: String -> current.copy(generating = false, notice = notice) }
                state = current.copy(generating = true, notice = null)
                failed
            }
            else -> return
        }
        val typed = feedUrl
        val typedCalendar = calendarUrl
        scope.launch {
            if (typed.isNotBlank() && typed.trim() != feedBaseline) {
                val saved = repository.saveFeedUrl(typed)
                if (saved.isFailure) {
                    if (feedUrl == typed) feedFieldError = FEED_DIDNT_LOAD
                    state = restore(FEED_DIDNT_LOAD)
                    return@launch
                }
                feedBaseline = typed.trim()
                if (feedUrl == typed) feedUrl = feedBaseline
                feedFieldError = null
            }
            var calendarNotice: String? = null
            if (typedCalendar.isNotBlank() && typedCalendar.trim() != calendarBaseline) {
                val saved = repository.saveCalendarUrl(typedCalendar)
                if (saved.isFailure) {
                    if (calendarUrl == typedCalendar) calendarFieldError = CALENDAR_LINK_DIDNT_OPEN
                    calendarNotice = CALENDAR_LINK_DIDNT_OPEN
                } else {
                    calendarBaseline = typedCalendar.trim()
                    if (calendarUrl == typedCalendar) calendarUrl = calendarBaseline
                    calendarFieldError = null
                }
            }
            val typedCalDavUrl = calDavUrl
            val typedCalDavUsername = calDavUsername
            val typedCalDavPassword = calDavPassword
            val calDavSaved = if (calDavFieldsDirty(typedCalDavUrl, typedCalDavUsername, typedCalDavPassword)) {
                persistCalDav(typedCalDavUrl, typedCalDavUsername, typedCalDavPassword)
            } else {
                true
            }
            val notice = if (!calDavSaved && calendarNotice == null) CALDAV_DIDNT_LOAD else calendarNotice
            val typedKey = geminiKey
            if (typedKey.trim() != geminiKeyBaseline) persistGeminiKey(typedKey)
            repository.generateToday()
                .onSuccess { edition ->
                    feedFieldError = null
                    state = EditionUiState.Ready(
                        edition,
                        notice = notice,
                        sameDayExtras = loadExtras(),
                    )
                }
                .onFailure {
                    state = restore(FEED_DIDNT_LOAD)
                }
        }
    }

    fun load() {
        state = EditionUiState.Loading
        scope.launch {
            repository.loadCurrentMorningEdition()
                .onSuccess { edition ->
                    state = EditionUiState.Ready(edition, sameDayExtras = loadExtras())
                }
                .onFailure { error -> state = mapFailure(error) }
        }
    }

    fun newEditionNow() {
        val current = state as? EditionUiState.Ready ?: return
        if (current.generating) return
        state = current.copy(generating = true, notice = null)
        scope.launch {
            repository.newEditionNow()
                .onSuccess {
                    val morning = repository.loadCurrentMorningEdition().getOrThrow()
                    state = EditionUiState.Ready(morning, sameDayExtras = loadExtras())
                }
                .onFailure {
                    val shown = state as? EditionUiState.Ready ?: current
                    state = shown.copy(generating = false, notice = FEED_DIDNT_LOAD)
                }
        }
    }

    fun openExtra(id: String) {
        val current = state as? EditionUiState.Ready ?: return
        val extra = current.sameDayExtras.singleOrNull { it.id == id } ?: return
        state = current.copy(edition = extra, taskSaved = false)
    }

    fun openMorning() {
        val current = state as? EditionUiState.Ready ?: return
        scope.launch {
            repository.loadCurrentMorningEdition()
                .onSuccess { morning ->
                    state = current.copy(
                        edition = morning,
                        sameDayExtras = loadExtras(),
                        taskSaved = false,
                    )
                }
        }
    }

    fun retrySample() {
        state = EditionUiState.Loading
        scope.launch {
            repository.retrySample()
                .onSuccess { edition -> state = EditionUiState.Ready(edition) }
                .onFailure { state = EditionUiState.SampleLoadFailed() }
        }
    }

    fun toggleTask(index: Int) {
        val current = state as? EditionUiState.Ready ?: return
        if (current.generating) return
        val tasks = current.edition.tasks.toMutableList()
        if (index !in tasks.indices) return
        val task = tasks[index]
        tasks[index] = task.copy(completed = !task.completed)
        persistEdition(current.edition.copy(tasks = tasks), current)
    }

    fun addTask(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val current = state as? EditionUiState.Ready ?: return
        if (current.generating) return
        val tasks = current.edition.tasks + EditionTask(title = trimmed)
        persistEdition(current.edition.copy(tasks = tasks), current)
    }

    fun saveTypewriter(text: String, onResult: (Boolean) -> Unit = {}) {
        val ready = state as? EditionUiState.Ready
        if (ready == null) {
            onResult(false)
            return
        }
        val updated = EditionTranscript.apply(ready.edition, text)
        if (updated == ready.edition) {
            typewriterError = null
            onResult(true)
            return
        }
        val generation = ++typewriterGeneration
        scope.launch {
            saveEdition(updated)
                .onSuccess { saved ->
                    if (generation != typewriterGeneration) {
                        onResult(false)
                        return@onSuccess
                    }
                    val current = state as? EditionUiState.Ready
                    if (current != null) state = current.copy(edition = saved)
                    typewriterError = null
                    typewriterSaved = true
                    onResult(true)
                }
                .onFailure {
                    if (generation != typewriterGeneration) {
                        onResult(false)
                        return@onFailure
                    }
                    typewriterError = EditionTranscript.SAVE_FAILED
                    onResult(false)
                }
        }
    }

    fun discardTypewriter(): String {
        typewriterGeneration += 1
        typewriterError = null
        val ready = state as? EditionUiState.Ready ?: return ""
        return EditionTranscript.render(ready.edition)
    }

    fun escapeTypewriter(text: String, close: () -> Unit) {
        saveTypewriter(text) { saved ->
            if (saved) close()
        }
    }

    private fun persistEdition(updated: Edition, previous: EditionUiState.Ready) {
        state = previous.copy(edition = updated)
        val rollback = previous
        scope.launch {
            saveEdition(updated)
                .onSuccess { saved ->
                    val current = state as? EditionUiState.Ready
                    if (current != null && current.edition.tasks == updated.tasks) {
                        state = current.copy(edition = saved, taskSaved = true)
                    }
                }
                .onFailure {
                    val current = state as? EditionUiState.Ready
                    if (current != null && current.edition.tasks == updated.tasks) {
                        state = rollback
                    }
                }
        }
    }

    private suspend fun loadExtras(): List<Edition> =
        runCatching { repository.loadSameDayExtras() }.getOrDefault(emptyList())

    private suspend fun saveEdition(edition: Edition): Result<Edition> =
        if (edition.role == EditionRole.extra) {
            repository.updateExtra(edition)
        } else {
            repository.updateMorningEdition(edition)
        }

    private fun persistGeminiKey(typed: String): Boolean {
        val saved = repository.saveGeminiKey(typed)
        if (saved.isSuccess) {
            geminiKeyBaseline = typed.trim()
            if (geminiKey == typed) geminiKey = geminiKeyBaseline
            return true
        }
        if (geminiKey == typed) geminiKey = geminiKeyBaseline
        return false
    }

    private fun calDavFieldsDirty(url: String, username: String, password: String): Boolean =
        password.isNotEmpty() || url.trim() != calDavUrlBaseline || username.trim() != calDavUsernameBaseline

    private suspend fun persistCalDav(typedUrl: String, typedUsername: String, typedPassword: String): Boolean {
        if (!calDavFieldsDirty(typedUrl, typedUsername, typedPassword)) return true
        val saved = repository.saveCalDavAccount(typedUrl, typedUsername, typedPassword)
        if (saved.isSuccess) {
            calDavUrlBaseline = typedUrl.trim()
            calDavUsernameBaseline = typedUsername.trim()
            if (calDavUrl == typedUrl) calDavUrl = calDavUrlBaseline
            if (calDavUsername == typedUsername) calDavUsername = calDavUsernameBaseline
            if (calDavPassword == typedPassword) calDavPassword = ""
            calDavFieldError = null
            return true
        }
        if (calDavUrl == typedUrl && calDavUsername == typedUsername && calDavPassword == typedPassword) {
            calDavFieldError = CALDAV_DIDNT_LOAD
        }
        return false
    }

    private fun mapFailure(error: Throwable): EditionUiState {
        if (error.message == "sample_load_failed") {
            return EditionUiState.SampleLoadFailed()
        }
        if (error.message == MORNING_EDITION_NOT_READY) {
            return EditionUiState.MissingMorning()
        }
        return EditionUiState.OtherError(error.message ?: "Couldn't load the edition.")
    }
}
