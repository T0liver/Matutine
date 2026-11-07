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

    data class Ready(val edition: Edition) : EditionUiState

    data class SampleLoadFailed(val message: String = "The sample edition didn't load.") : EditionUiState

    data class OtherError(val message: String) : EditionUiState
}

class EditionViewModel(
    private val repository: EditionRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) {
    var state: EditionUiState by mutableStateOf(EditionUiState.Loading)
        private set

    init {
        load()
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
