package hu.toliver.matutine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import hu.toliver.matutine.edition.EditionScreen
import hu.toliver.matutine.edition.EditionUiState
import hu.toliver.matutine.edition.EditionViewModel
import hu.toliver.matutine.edition.LocalEditionRepository
import hu.toliver.matutine.edition.readFeedDocument
import hu.toliver.matutine.edition.showsNoTopicLine
import hu.toliver.matutine.settings.InterestsScreen
import hu.toliver.matutine.settings.SettingsScreen
import hu.toliver.matutine.store.FileEditionStore
import hu.toliver.matutine.theme.MatutineTheme

private sealed interface MatutineScreen {
    data object Edition : MatutineScreen
    data object Settings : MatutineScreen
    data class Interests(val returnToSettings: Boolean) : MatutineScreen
}

@Composable
fun MatutineApp(dataRoot: String) {
    val viewModel = remember(dataRoot) {
        val store = FileEditionStore(dataRoot)
        val repository = LocalEditionRepository(store, readFeed = ::readFeedDocument)
        EditionViewModel(repository)
    }
    var screen by remember { mutableStateOf<MatutineScreen>(MatutineScreen.Edition) }
    val ready = viewModel.state as? EditionUiState.Ready
    MatutineTheme {
        when (val current = screen) {
            MatutineScreen.Settings -> SettingsScreen(
                feedUrl = viewModel.feedUrl,
                onFeedUrlChange = viewModel::updateFeedUrl,
                feedError = viewModel.feedFieldError,
                onSaveFeed = viewModel::saveFeed,
                onOpenInterests = { screen = MatutineScreen.Interests(returnToSettings = true) },
                onGenerate = {
                    viewModel.generate()
                    screen = MatutineScreen.Edition
                },
                onBack = { screen = MatutineScreen.Edition },
            )
            is MatutineScreen.Interests -> InterestsScreen(
                topics = viewModel.topics,
                onSave = { topics ->
                    if (viewModel.saveTopics(topics)) {
                        screen = if (current.returnToSettings) {
                            MatutineScreen.Settings
                        } else {
                            MatutineScreen.Edition
                        }
                    }
                },
                onBack = {
                    screen = if (current.returnToSettings) {
                        MatutineScreen.Settings
                    } else {
                        MatutineScreen.Edition
                    }
                },
            )
            MatutineScreen.Edition -> EditionScreen(
                state = viewModel.state,
                onRetrySample = viewModel::retrySample,
                onOpenSettings = { screen = MatutineScreen.Settings },
                showNoTopicLine = ready?.let {
                    showsNoTopicLine(it.edition.isSample, viewModel.topics)
                } == true,
                onOpenInterests = { screen = MatutineScreen.Interests(returnToSettings = false) },
            )
        }
    }
}

@Composable
@Preview
fun MatutineAppPreview() {
    MatutineApp(dataRoot = ".")
}
