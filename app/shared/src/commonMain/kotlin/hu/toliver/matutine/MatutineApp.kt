package hu.toliver.matutine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import hu.toliver.matutine.edition.EditionScreen
import hu.toliver.matutine.edition.EditionUiState
import hu.toliver.matutine.edition.EditionViewModel
import hu.toliver.matutine.edition.LocalEditionRepository
import hu.toliver.matutine.edition.calendarPermissionIsOff
import hu.toliver.matutine.edition.openCalendarPermissionSettings
import hu.toliver.matutine.edition.readCalDavCalendar
import hu.toliver.matutine.edition.readDeviceCalendar
import hu.toliver.matutine.edition.readFeedDocument
import hu.toliver.matutine.edition.showsNoTopicLine
import hu.toliver.matutine.edition.writesDeviceCalendarSnapshot
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
        val repository = LocalEditionRepository(
            store,
            readFeed = ::readFeedDocument,
            writesDeviceCalendarSnapshot = writesDeviceCalendarSnapshot(),
            readDeviceCalendar = ::readDeviceCalendar,
            readCalDav = ::readCalDavCalendar,
        )
        EditionViewModel(repository)
    }
    var screen by remember { mutableStateOf<MatutineScreen>(MatutineScreen.Edition) }
    val calendarPermissionOff = rememberCalendarPermissionOff()
    val ready = viewModel.state as? EditionUiState.Ready
    MatutineTheme {
        when (val current = screen) {
            MatutineScreen.Settings -> SettingsScreen(
                feedUrl = viewModel.feedUrl,
                onFeedUrlChange = viewModel::updateFeedUrl,
                feedError = viewModel.feedFieldError,
                onSaveFeed = viewModel::saveFeed,
                calendarUrl = viewModel.calendarUrl,
                onCalendarUrlChange = viewModel::updateCalendarUrl,
                calendarError = viewModel.calendarFieldError,
                onSaveCalendar = viewModel::saveCalendar,
                calDavUrl = viewModel.calDavUrl,
                onCalDavUrlChange = viewModel::updateCalDavUrl,
                calDavUsername = viewModel.calDavUsername,
                onCalDavUsernameChange = viewModel::updateCalDavUsername,
                calDavPassword = viewModel.calDavPassword,
                onCalDavPasswordChange = viewModel::updateCalDavPassword,
                calDavError = viewModel.calDavFieldError,
                onSaveCalDav = viewModel::saveCalDav,
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
                calendarPermissionOff = calendarPermissionOff,
                onOpenCalendarPermission = ::openCalendarPermissionSettings,
            )
        }
    }
}

@Composable
private fun rememberCalendarPermissionOff(): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var off by remember { mutableStateOf(calendarPermissionIsOff()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) off = calendarPermissionIsOff()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return off
}

@Composable
@Preview
fun MatutineAppPreview() {
    MatutineApp(dataRoot = ".")
}
