package hu.toliver.matutine.admin

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import hu.toliver.matutine.edition.chromeControlMinHeight
import hu.toliver.matutine.theme.LocalMatutineEditionTypography
import hu.toliver.matutine.theme.NewsprintColors

@Composable
fun AdminScreen(
    feedUrl: String,
    onFeedUrlChange: (String) -> Unit,
    feedError: String?,
    onSaveFeed: () -> Unit,
    calendarUrl: String,
    onCalendarUrlChange: (String) -> Unit,
    calendarError: String?,
    onSaveCalendar: () -> Unit,
    calDavUrl: String,
    onCalDavUrlChange: (String) -> Unit,
    calDavUsername: String,
    onCalDavUsernameChange: (String) -> Unit,
    calDavPassword: String,
    onCalDavPasswordChange: (String) -> Unit,
    calDavError: String?,
    onSaveCalDav: () -> Unit,
    generationTime: String,
    onGenerationTimeChange: (String) -> Unit,
    generationTimeError: String?,
    onSaveGenerationTime: () -> Unit,
    timezone: String,
    onTimezoneChange: (String) -> Unit,
    onSaveTimezone: () -> Unit,
    aiProvider: String,
    geminiKey: String,
    onGeminiKeyChange: (String) -> Unit,
    onSaveGeminiKey: () -> Unit,
    sharedToken: String,
    onSharedTokenChange: (String) -> Unit,
    onSaveSharedToken: () -> Unit,
    onOpenInterests: () -> Unit,
    onGenerate: () -> Unit,
    generating: Boolean,
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
        Text(text = "Admin", style = typography.story.copy(color = NewsprintColors.ink))
        labeledField("Feed", feedUrl, onFeedUrlChange, feedError, KeyboardType.Uri, ImeAction.Done, onSaveFeed, controlHeight)
        labeledField(
            "Calendar",
            calendarUrl,
            onCalendarUrlChange,
            calendarError,
            KeyboardType.Uri,
            ImeAction.Done,
            onSaveCalendar,
            controlHeight,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "CalDAV", style = typography.body.copy(color = NewsprintColors.ink))
            Text(text = "URL", style = typography.body.copy(color = NewsprintColors.ink))
            adminField(calDavUrl, onCalDavUrlChange, calDavError != null, KeyboardType.Uri, ImeAction.Next)
            Text(text = "Username", style = typography.body.copy(color = NewsprintColors.ink))
            adminField(calDavUsername, onCalDavUsernameChange, calDavError != null, KeyboardType.Text, ImeAction.Next)
            Text(text = "Password", style = typography.body.copy(color = NewsprintColors.ink))
            adminField(
                calDavPassword,
                onCalDavPasswordChange,
                calDavError != null,
                KeyboardType.Password,
                ImeAction.Done,
                onDone = onSaveCalDav,
                visualTransformation = PasswordVisualTransformation(),
            )
            if (calDavError != null) {
                Text(text = calDavError, style = typography.body.copy(color = NewsprintColors.error))
            }
        }
        labeledField(
            "Generation time",
            generationTime,
            onGenerationTimeChange,
            generationTimeError,
            KeyboardType.Text,
            ImeAction.Done,
            onSaveGenerationTime,
            controlHeight,
        )
        labeledField(
            "Timezone",
            timezone,
            onTimezoneChange,
            null,
            KeyboardType.Text,
            ImeAction.Done,
            onSaveTimezone,
            controlHeight,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "AI provider", style = typography.body.copy(color = NewsprintColors.ink))
            Text(text = aiProvider, style = typography.body.copy(color = NewsprintColors.inkSecondary))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Gemini key", style = typography.body.copy(color = NewsprintColors.ink))
            adminField(
                geminiKey,
                onGeminiKeyChange,
                false,
                KeyboardType.Password,
                ImeAction.Done,
                onDone = onSaveGeminiKey,
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        labeledField(
            "Shared token",
            sharedToken,
            onSharedTokenChange,
            null,
            KeyboardType.Text,
            ImeAction.Done,
            onSaveSharedToken,
            controlHeight,
        )
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
            enabled = !generating,
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
                text = if (generating) "Setting today's edition." else "Generate today's edition",
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
            Text(text = "Return to the edition", style = typography.body.copy(color = NewsprintColors.ink))
        }
    }
}

@Composable
private fun labeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onDone: () -> Unit,
    controlHeight: androidx.compose.ui.unit.Dp,
) {
    val typography = LocalMatutineEditionTypography.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = label, style = typography.body.copy(color = NewsprintColors.ink))
        adminField(value, onValueChange, error != null, keyboardType, imeAction, onDone)
        if (error != null) {
            Text(text = error, style = typography.body.copy(color = NewsprintColors.error))
        }
    }
}

@Composable
private fun adminField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onDone: () -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val typography = LocalMatutineEditionTypography.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = chromeControlMinHeight()),
        singleLine = true,
        isError = isError,
        textStyle = typography.body,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
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
}
