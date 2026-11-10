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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import hu.toliver.matutine.edition.Edition
import hu.toliver.matutine.edition.EditionScreen
import hu.toliver.matutine.edition.EditionUiState
import hu.toliver.matutine.edition.decodeEdition
import hu.toliver.matutine.edition.webPaperGate
import hu.toliver.matutine.theme.EditionThemeColors
import hu.toliver.matutine.theme.LocalMatutineEditionTypography
import hu.toliver.matutine.theme.MatutineTheme
import hu.toliver.matutine.theme.NewsprintColors
import kotlinx.coroutines.launch

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
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    MatutineTheme {
        val paper = edition
        if (paper == null) {
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
                            if (gate.showEdition) {
                                decodeEdition(response.body)
                            } else {
                                null
                            }
                        } catch (_: Throwable) {
                            message = null
                            null
                        }
                        edition = decision
                        submitting = false
                    }
                },
            )
        } else {
            EditionScreen(
                state = EditionUiState.Ready(paper),
                onRetrySample = {},
                onOpenSettings = {},
                showNoTopicLine = false,
                onOpenInterests = {},
                onToggleTask = {},
                onAddTask = {},
                readOnly = true,
            )
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
