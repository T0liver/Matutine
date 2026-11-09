package hu.toliver.matutine.edition

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

sealed interface EditionUiState {
    data object Loading : EditionUiState

    data class Ready(
        val edition: Edition,
        val generating: Boolean = false,
        val notice: String? = null,
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

    private var feedBaseline: String = ""
    private var calendarBaseline: String = ""
    private var calDavUrlBaseline: String = ""
    private var calDavUsernameBaseline: String = ""

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
        load()
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
            repository.generateToday()
                .onSuccess { edition ->
                    feedFieldError = null
                    state = EditionUiState.Ready(edition, notice = notice)
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
                .onSuccess { edition -> state = EditionUiState.Ready(edition) }
                .onFailure { error -> state = mapFailure(error) }
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
