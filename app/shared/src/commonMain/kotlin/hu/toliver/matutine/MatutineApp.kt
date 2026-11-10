package hu.toliver.matutine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import hu.toliver.matutine.edition.EditionScreen
import hu.toliver.matutine.edition.EditionTranscript
import hu.toliver.matutine.edition.EditionUiState
import hu.toliver.matutine.edition.EditionViewModel
import hu.toliver.matutine.edition.TypewriterPane
import hu.toliver.matutine.edition.showsTypewriter
import hu.toliver.matutine.edition.LocalEditionRepository
import hu.toliver.matutine.edition.calendarPermissionIsOff
import hu.toliver.matutine.edition.openCalendarPermissionSettings
import hu.toliver.matutine.edition.readCalDavCalendar
import hu.toliver.matutine.edition.readDeviceCalendar
import hu.toliver.matutine.edition.readFeedDocument
import hu.toliver.matutine.edition.summarizeWithGemini
import hu.toliver.matutine.edition.showsNoTopicLine
import hu.toliver.matutine.edition.writesDeviceCalendarSnapshot
import hu.toliver.matutine.settings.InterestsScreen
import hu.toliver.matutine.settings.SettingsScreen
import hu.toliver.matutine.settings.showsAndroidPaperCustomization
import hu.toliver.matutine.settings.showsNotificationTimeRow
import hu.toliver.matutine.store.FileEditionStore
import hu.toliver.matutine.theme.MatutineTheme
import hu.toliver.matutine.theme.resolveEditionTheme

private sealed interface MatutineScreen {
    data object Edition : MatutineScreen
    data object Settings : MatutineScreen
    data object Typewriter : MatutineScreen
    data class Interests(val returnToSettings: Boolean) : MatutineScreen
}

class WindowFlush {
    var saveOpenTypewriter: ((() -> Unit) -> Unit)? = null
}

@Composable
fun MatutineApp(
    dataRoot: String,
    trayOpenRequest: Int = 0,
    trayGenerateRequest: Int = 0,
    windowFlush: WindowFlush = WindowFlush(),
    typewriter: @Composable (TypewriterPane) -> Unit = {},
) {
    val viewModel = remember(dataRoot) {
        val store = FileEditionStore(dataRoot)
        val repository = LocalEditionRepository(
            store,
            readFeed = ::readFeedDocument,
            writesDeviceCalendarSnapshot = writesDeviceCalendarSnapshot(),
            readDeviceCalendar = ::readDeviceCalendar,
            readCalDav = ::readCalDavCalendar,
            summarize = ::summarizeWithGemini,
        )
        EditionViewModel(repository)
    }
    var screen by remember { mutableStateOf<MatutineScreen>(MatutineScreen.Edition) }
    var typewriterDraft by remember { mutableStateOf("") }
    LaunchedEffect(trayOpenRequest) {
        if (trayOpenRequest > 0) {
            if (screen == MatutineScreen.Typewriter) {
                viewModel.escapeTypewriter(typewriterDraft) { screen = MatutineScreen.Edition }
            } else {
                screen = MatutineScreen.Edition
            }
        }
    }
    LaunchedEffect(trayGenerateRequest) {
        if (trayGenerateRequest > 0) {
            if (screen == MatutineScreen.Typewriter) {
                viewModel.escapeTypewriter(typewriterDraft) {
                    viewModel.generate()
                    screen = MatutineScreen.Edition
                }
            } else {
                viewModel.generate()
                screen = MatutineScreen.Edition
            }
        }
    }
    SideEffect {
        windowFlush.saveOpenTypewriter = if (screen == MatutineScreen.Typewriter) {
            { done -> viewModel.saveTypewriter(typewriterDraft) { done() } }
        } else {
            null
        }
    }
    val calendarPermissionOff = rememberCalendarPermissionOff()
    val ready = viewModel.state as? EditionUiState.Ready
    val paperSettings = showsAndroidPaperCustomization()
    MatutineTheme(editionColors = resolveEditionTheme(viewModel.editionTheme)) {
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
                geminiKey = viewModel.geminiKey,
                onGeminiKeyChange = viewModel::updateGeminiKey,
                onSaveGeminiKey = viewModel::saveGeminiKey,
                onOpenInterests = { screen = MatutineScreen.Interests(returnToSettings = true) },
                onGenerate = {
                    viewModel.generate()
                    screen = MatutineScreen.Edition
                },
                onBack = { screen = MatutineScreen.Edition },
                notificationTime = if (showsNotificationTimeRow()) viewModel.notificationTime else null,
                onNotificationTimeChange = if (showsNotificationTimeRow()) viewModel::updateNotificationTime else null,
                notificationTimeError = if (showsNotificationTimeRow()) viewModel.notificationTimeError else null,
                onSaveNotificationTime = if (showsNotificationTimeRow()) viewModel::saveNotificationTime else null,
                notificationBody = if (paperSettings) viewModel.notificationBody else null,
                onNotificationBodyChange = if (paperSettings) viewModel::updateNotificationBody else null,
                onSaveNotificationBody = if (paperSettings) viewModel::saveNotificationBody else null,
                editionThemeId = if (paperSettings) viewModel.editionTheme else null,
                onEditionThemeSelect = if (paperSettings) viewModel::saveEditionTheme else null,
                editionLayoutId = if (paperSettings) viewModel.editionLayout else null,
                onEditionLayoutSelect = if (paperSettings) viewModel::saveEditionLayout else null,
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
                onToggleTask = viewModel::toggleTask,
                onAddTask = viewModel::addTask,
                calendarPermissionOff = calendarPermissionOff,
                onOpenCalendarPermission = ::openCalendarPermissionSettings,
                editionLayoutId = viewModel.editionLayout,
                sameDayExtras = ready?.sameDayExtras.orEmpty(),
                onNewEditionNow = viewModel::newEditionNow,
                onOpenExtra = viewModel::openExtra,
                onOpenMorning = viewModel::openMorning,
                onOpenTypewriter = if (showsTypewriter() && ready != null) {
                    {
                        typewriterDraft = EditionTranscript.render(ready.edition)
                        screen = MatutineScreen.Typewriter
                    }
                } else {
                    null
                },
            )
            MatutineScreen.Typewriter -> typewriter(
                TypewriterPane(
                    text = typewriterDraft,
                    saved = viewModel.typewriterSaved,
                    error = viewModel.typewriterError,
                    onTextChange = { typewriterDraft = it },
                    onPause = { viewModel.saveTypewriter(it) },
                    onDiscard = { typewriterDraft = viewModel.discardTypewriter() },
                    onEscape = { viewModel.escapeTypewriter(typewriterDraft) { screen = MatutineScreen.Edition } },
                ),
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
