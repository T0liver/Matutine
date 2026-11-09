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

    var topics by mutableStateOf<List<String>>(emptyList())
        private set

    private var feedBaseline: String = ""

    init {
        feedBaseline = runCatching { repository.savedFeedUrls().singleOrNull().orEmpty() }.getOrDefault("")
        feedUrl = feedBaseline
        topics = runCatching { repository.savedTopics() }.getOrDefault(emptyList())
        load()
    }

    fun updateFeedUrl(value: String) {
        if (value != feedUrl) feedFieldError = null
        feedUrl = value
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

    fun saveTopics(values: List<String>): Boolean =
        runCatching {
            repository.saveTopics(values)
            topics = repository.savedTopics()
        }.isSuccess

    fun generate() {
        val current = state as? EditionUiState.Ready ?: return
        if (current.generating) return
        val typed = feedUrl
        state = current.copy(generating = true, notice = null)
        scope.launch {
            if (typed.isNotBlank() && typed.trim() != feedBaseline) {
                val saved = repository.saveFeedUrl(typed)
                if (saved.isFailure) {
                    if (feedUrl == typed) feedFieldError = FEED_DIDNT_LOAD
                    state = current.copy(generating = false, notice = FEED_DIDNT_LOAD)
                    return@launch
                }
                feedBaseline = typed.trim()
                if (feedUrl == typed) feedUrl = feedBaseline
                feedFieldError = null
            }
            repository.generateToday()
                .onSuccess { edition ->
                    feedFieldError = null
                    state = EditionUiState.Ready(edition)
                }
                .onFailure {
                    state = current.copy(generating = false, notice = FEED_DIDNT_LOAD)
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

    private fun mapFailure(error: Throwable): EditionUiState {
        if (error.message == "sample_load_failed") {
            return EditionUiState.SampleLoadFailed()
        }
        return EditionUiState.OtherError(error.message ?: "Couldn't load the edition.")
    }
}
