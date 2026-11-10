package hu.toliver.matutine

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import hu.toliver.matutine.theme.EditionThemeColors
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
    ComposeViewport {
        ServerPaperApp()
    }
}

@Composable
fun ServerPaperApp() {
    var typedToken by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var edition by remember { mutableStateOf<Edition?>(null) }
    var webEdition by remember { mutableStateOf<WebEditionModel?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var screen by remember { mutableStateOf<WebPaperScreen>(WebPaperScreen.Edition) }
    val scope = rememberCoroutineScope()
    val admin = remember(typedToken) { ServerAdminModel(typedToken) }
    var adminTopics by remember { mutableStateOf<List<String>>(emptyList()) }
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
    MatutineTheme {
        val paperModel = webEdition
        val paper = paperModel?.edition ?: edition
        if (paper == null || paperModel == null) {
            TokenGate(
                token = typedToken,
                onTokenChange = { typedToken = it },
                message = message,
                submitting = submitting,
                onSubmit = {
                    if (submitting) return@TokenGate
                    submitting = true
                    scope.launch {
                        val decision = try {
                            val response = fetchCurrentEdition(typedToken)
                            val gate = webPaperGate(typedToken, response.status)
                            typedToken = gate.typedToken
                            message = gate.message
                            if (gate.showEdition) decodeEdition(response.body) else null
                        } catch (_: Throwable) {
                            message = null
                            null
                        }
                        edition = decision
                        webEdition = decision?.let { WebEditionModel(typedToken, it, scope) }
                        submitting = false
                    }
                },
            )
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
private fun TokenGate(
    token: String,
    onTokenChange: (String) -> Unit,
    message: String?,
    submitting: Boolean,
    onSubmit: () -> Unit,
) {
    val typography = LocalMatutineEditionTypography.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NewsprintColors.chrome)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Token", style = typography.body.copy(color = EditionThemeColors.current.ink))
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp),
            singleLine = true,
            textStyle = typography.body.copy(color = EditionThemeColors.current.ink),
            shape = RoundedCornerShape(2.dp),
        )
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                style = typography.body.copy(color = EditionThemeColors.current.error),
            )
        }
        Button(
            onClick = onSubmit,
            enabled = !submitting,
            modifier = Modifier.heightIn(min = 40.dp),
            shape = RoundedCornerShape(2.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EditionThemeColors.current.ink,
                contentColor = EditionThemeColors.current.paper,
            ),
        ) {
            Text(text = "Continue")
        }
    }
}
