package hu.toliver.matutine.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hu.toliver.matutine.edition.chromeControlMinHeight
import hu.toliver.matutine.theme.LocalMatutineEditionTypography
import hu.toliver.matutine.theme.NewsprintColors

@Composable
fun SettingsScreen(
    feedUrl: String,
    onFeedUrlChange: (String) -> Unit,
    feedError: String?,
    onSaveFeed: () -> Unit,
    onOpenInterests: () -> Unit,
    onGenerate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = LocalMatutineEditionTypography.current
    val controlHeight = chromeControlMinHeight()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NewsprintColors.chrome)
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Settings", style = typography.story.copy(color = NewsprintColors.ink))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = controlHeight),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Feed", style = typography.body.copy(color = NewsprintColors.ink))
            OutlinedTextField(
                value = feedUrl,
                onValueChange = onFeedUrlChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = controlHeight),
                singleLine = true,
                isError = feedError != null,
                textStyle = typography.body,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSaveFeed() }),
                shape = RoundedCornerShape(4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NewsprintColors.ink,
                    unfocusedBorderColor = NewsprintColors.ink,
                    focusedContainerColor = NewsprintColors.field,
                    unfocusedContainerColor = NewsprintColors.field,
                    cursorColor = NewsprintColors.ink,
                    focusedTextColor = NewsprintColors.ink,
                    unfocusedTextColor = NewsprintColors.ink,
                    errorBorderColor = NewsprintColors.error,
                    errorCursorColor = NewsprintColors.error,
                    errorTextColor = NewsprintColors.ink,
                ),
            )
            if (feedError != null) {
                Text(
                    text = feedError,
                    style = typography.body.copy(color = NewsprintColors.error),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = controlHeight)
                .clickable(role = Role.Button, onClick = onOpenInterests),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Interests", style = typography.body.copy(color = NewsprintColors.ink))
        }
        HorizontalDivider(thickness = 1.dp, color = NewsprintColors.rule)
        Button(
            onClick = onGenerate,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = controlHeight),
            colors = ButtonDefaults.buttonColors(
                containerColor = NewsprintColors.ink,
                contentColor = NewsprintColors.paper,
            ),
            shape = RoundedCornerShape(4.dp),
        ) {
            Text(
                text = "Generate today's edition",
                style = typography.body.copy(color = NewsprintColors.paper),
            )
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = controlHeight),
            border = BorderStroke(1.dp, NewsprintColors.ink),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = NewsprintColors.paper,
                contentColor = NewsprintColors.ink,
            ),
            shape = RoundedCornerShape(4.dp),
        ) {
            Text(
                text = "Return to the edition",
                style = typography.body.copy(color = NewsprintColors.ink),
            )
        }
    }
}
