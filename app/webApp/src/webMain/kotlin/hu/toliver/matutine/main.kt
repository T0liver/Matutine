package hu.toliver.matutine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import hu.toliver.matutine.admin.AdminScreen
import hu.toliver.matutine.edition.Edition
import hu.toliver.matutine.edition.EditionScreen
import hu.toliver.matutine.edition.LightEditHooks
import hu.toliver.matutine.edition.EditionUiState
import hu.toliver.matutine.edition.decodeEdition
import hu.toliver.matutine.edition.decodeReaderSettings
import hu.toliver.matutine.edition.webPaperGate
import hu.toliver.matutine.settings.InterestsScreen
import hu.toliver.matutine.theme.LocalMatutineEditionTypography
import hu.toliver.matutine.theme.MatutineTheme
import hu.toliver.matutine.theme.NewsprintColors
import kotlinx.coroutines.launch

private sealed interface WebPaperScreen {
    data object Edition : WebPaperScreen
    data object Admin : WebPaperScreen
    data object Interests : WebPaperScreen
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val tokenFromDom = consumeMatutineBootstrap()?.token
    val tokenFromSession = readStoredBearerToken()
    val initialToken = tokenFromDom ?: tokenFromSession.orEmpty()
    val initialEditionJson = consumeBootstrapEditionJson()
    ComposeViewport {
        ServerPaperApp(initialToken = initialToken, initialEditionJson = initialEditionJson)
    }
}

@Composable
fun ServerPaperApp(
    initialToken: String = "",
    initialEditionJson: String? = null,
) {
    var typedToken by remember { mutableStateOf(initialToken) }
    var edition by remember { mutableStateOf<Edition?>(null) }
    var webEdition by remember { mutableStateOf<WebEditionModel?>(null) }
    var gateSubmitRequest by remember { mutableIntStateOf(0) }
    var screen by remember { mutableStateOf<WebPaperScreen>(WebPaperScreen.Edition) }
    val scope = rememberCoroutineScope()
    val admin = remember(typedToken) { ServerAdminModel(typedToken) }
    var adminTopics by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(initialToken, initialEditionJson) {
        if (initialEditionJson != null && initialToken.isNotBlank()) {
            val decoded = decodeEdition(initialEditionJson)
            typedToken = initialToken
            edition = decoded
            webEdition = WebEditionModel(initialToken, decoded, scope)
            return@LaunchedEffect
        }
        if (initialToken.isBlank() || edition != null) return@LaunchedEffect
        gateSubmitRequest = 1
    }
    LaunchedEffect(edition, typedToken) {
        if (edition != null && typedToken.isNotBlank()) {
            val response = apiGet("/settings", typedToken)
            if (response.status == 200) {
                adminTopics = decodeReaderSettings(response.body).topics
            }
        }
    }
    LaunchedEffect(screen) {
        if (screen == WebPaperScreen.Admin) {
            admin.load()
            adminTopics = admin.topics
        }
    }
    LaunchedEffect(gateSubmitRequest) {
        if (gateSubmitRequest == 0) return@LaunchedEffect
        val submittedToken = typedToken
        if (submittedToken.isBlank()) return@LaunchedEffect
        val decision = try {
            val response = fetchCurrentEdition(submittedToken)
            val gate = webPaperGate(submittedToken, response.status)
            typedToken = gate.typedToken
            if (gate.showEdition) decodeEdition(response.body) else null
        } catch (_: Throwable) {
            null
        }
        if (decision != null) {
            edition = decision
            webEdition = WebEditionModel(typedToken, decision, scope)
        }
    }
    MatutineTheme {
        val paperModel = webEdition
        val paper = paperModel?.edition ?: edition
        if (paper == null || paperModel == null) {
            OpeningPaperChrome()
        } else {
            when (screen) {
                WebPaperScreen.Edition -> EditionScreen(
                    state = EditionUiState.Ready(paper),
                    onRetrySample = {},
                    onOpenSettings = {},
                    showNoTopicLine = !paper.isSample && adminTopics.isEmpty(),
                    onOpenInterests = {},
                    onToggleTask = {},
                    onAddTask = {},
                    readOnly = true,
                    onOpenAdmin = { screen = WebPaperScreen.Admin },
                    lightEdit = LightEditHooks(
                        saved = paperModel.lightEditSaved,
                        error = paperModel.lightEditError,
                        resetKey = paperModel.lightEditResetKey,
                        onCommit = paperModel::commit,
                        onDiscard = paperModel::discardLightEdit,
                    ),
                )
                WebPaperScreen.Admin -> AdminScreen(
                    feedUrl = admin.feedUrl,
                    onFeedUrlChange = admin::updateFeedUrl,
                    feedError = admin.feedFieldError,
                    onSaveFeed = { scope.launch { admin.saveFeed() } },
                    calendarUrl = admin.calendarUrl,
                    onCalendarUrlChange = admin::updateCalendarUrl,
                    calendarError = admin.calendarFieldError,
                    onSaveCalendar = { scope.launch { admin.saveCalendar() } },
                    calDavUrl = admin.calDavUrl,
                    onCalDavUrlChange = admin::updateCalDavUrl,
                    calDavUsername = admin.calDavUsername,
                    onCalDavUsernameChange = admin::updateCalDavUsername,
                    calDavPassword = admin.calDavPassword,
                    onCalDavPasswordChange = admin::updateCalDavPassword,
                    calDavError = admin.calDavFieldError,
                    onSaveCalDav = { scope.launch { admin.saveCalDav() } },
                    generationTime = admin.generationTime,
                    onGenerationTimeChange = admin::updateGenerationTime,
                    generationTimeError = admin.generationTimeError,
                    onSaveGenerationTime = { scope.launch { admin.saveGenerationTime() } },
                    timezone = admin.timezone,
                    onTimezoneChange = admin::updateTimezone,
                    onSaveTimezone = { scope.launch { admin.saveTimezone() } },
                    aiProvider = admin.aiProvider,
                    geminiKey = admin.geminiKey,
                    onGeminiKeyChange = admin::updateGeminiKey,
                    onSaveGeminiKey = { scope.launch { admin.saveGeminiKey() } },
                    sharedToken = admin.sharedToken,
                    onSharedTokenChange = admin::updateSharedToken,
                    onSaveSharedToken = { scope.launch { admin.saveSharedToken() } },
                    onOpenInterests = { screen = WebPaperScreen.Interests },
                    generating = admin.generating,
                    onGenerate = {
                        scope.launch {
                            val next = admin.generateEdition()
                            if (next != null) {
                                edition = next
                                paperModel.replaceEdition(next)
                                adminTopics = admin.topics
                                screen = WebPaperScreen.Edition
                            }
                        }
                    },
                    onBack = { screen = WebPaperScreen.Edition },
                )
                WebPaperScreen.Interests -> InterestsScreen(
                    topics = admin.topics,
                    onSave = { topics ->
                        scope.launch {
                            if (admin.saveTopics(topics)) {
                                adminTopics = topics
                                screen = WebPaperScreen.Admin
                            }
                        }
                    },
                    onBack = { screen = WebPaperScreen.Admin },
                )
            }
        }
    }
}

@Composable
private fun OpeningPaperChrome() {
    val typography = LocalMatutineEditionTypography.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NewsprintColors.chrome),
    ) {
        if (readStoredBearerToken().isNullOrBlank()) {
            Text(
                text = "Reload this page to enter a token.",
                style = typography.body.copy(color = NewsprintColors.ink),
            )
        }
    }
}