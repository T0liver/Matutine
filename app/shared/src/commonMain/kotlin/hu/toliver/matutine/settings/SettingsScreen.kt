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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import hu.toliver.matutine.edition.EDITION_LAYOUT_OPTIONS
import hu.toliver.matutine.edition.EDITION_THEME_OPTIONS
import hu.toliver.matutine.edition.chromeControlMinHeight
import hu.toliver.matutine.theme.LocalMatutineEditionTypography
import hu.toliver.matutine.theme.NewsprintColors

@Composable
fun SettingsScreen(
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
    geminiKey: String,
    onGeminiKeyChange: (String) -> Unit,
    onSaveGeminiKey: () -> Unit,
    onOpenInterests: () -> Unit,
    onGenerate: () -> Unit,
    onBack: () -> Unit,
    notificationTime: String? = null,
    onNotificationTimeChange: ((String) -> Unit)? = null,
    notificationTimeError: String? = null,
    onSaveNotificationTime: (() -> Unit)? = null,
    notificationBody: String? = null,
    onNotificationBodyChange: ((String) -> Unit)? = null,
    onSaveNotificationBody: (() -> Unit)? = null,
    editionThemeId: String? = null,
    onEditionThemeSelect: ((String) -> Unit)? = null,
    editionLayoutId: String? = null,
    onEditionLayoutSelect: ((String) -> Unit)? = null,
    showServerLink: Boolean = false,
    serverBaseUrl: String = "",
    onServerBaseUrlChange: (String) -> Unit = {},
    serverToken: String = "",
    onServerTokenChange: (String) -> Unit = {},
    serverAddressError: String? = null,
    onSaveServer: () -> Unit = {},
    onForgetServer: (() -> Unit)? = null,
    showSources: Boolean = true,
    showGeminiKey: Boolean = true,
    showInterests: Boolean = true,
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
        if (showServerLink) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = controlHeight),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "Server", style = typography.body.copy(color = NewsprintColors.ink))
                newsprintField(
                    value = serverBaseUrl,
                    onValueChange = onServerBaseUrlChange,
                    isError = serverAddressError != null,
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                )
                if (serverAddressError != null) {
                    Text(
                        text = serverAddressError,
                        style = typography.body.copy(color = NewsprintColors.error),
                    )
                }
                Text(text = "Token", style = typography.body.copy(color = NewsprintColors.ink))
                newsprintField(
                    value = serverToken,
                    onValueChange = onServerTokenChange,
                    isError = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    onDone = onSaveServer,
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedButton(
                    onClick = onSaveServer,
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
                    Text(text = "Save server", style = typography.body.copy(color = NewsprintColors.ink))
                }
                if (onForgetServer != null) {
                    OutlinedButton(
                        onClick = onForgetServer,
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
                        Text(text = "Forget server", style = typography.body.copy(color = NewsprintColors.ink))
                    }
                }
            }
        }
        if (showSources) {
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = controlHeight),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Calendar", style = typography.body.copy(color = NewsprintColors.ink))
            OutlinedTextField(
                value = calendarUrl,
                onValueChange = onCalendarUrlChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = controlHeight),
                singleLine = true,
                isError = calendarError != null,
                textStyle = typography.body,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSaveCalendar() }),
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
            if (calendarError != null) {
                Text(
                    text = calendarError,
                    style = typography.body.copy(color = NewsprintColors.error),
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = controlHeight),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "CalDAV", style = typography.body.copy(color = NewsprintColors.ink))
            Text(text = "URL", style = typography.body.copy(color = NewsprintColors.ink))
            newsprintField(
                value = calDavUrl,
                onValueChange = onCalDavUrlChange,
                isError = calDavError != null,
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next,
            )
            Text(text = "Username", style = typography.body.copy(color = NewsprintColors.ink))
            newsprintField(
                value = calDavUsername,
                onValueChange = onCalDavUsernameChange,
                isError = calDavError != null,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            )
            Text(text = "Password", style = typography.body.copy(color = NewsprintColors.ink))
            newsprintField(
                value = calDavPassword,
                onValueChange = onCalDavPasswordChange,
                isError = calDavError != null,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onDone = onSaveCalDav,
                visualTransformation = PasswordVisualTransformation(),
            )
            if (calDavError != null) {
                Text(
                    text = calDavError,
                    style = typography.body.copy(color = NewsprintColors.error),
                )
            }
        }
        }
        if (showGeminiKey) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = controlHeight),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Gemini key", style = typography.body.copy(color = NewsprintColors.ink))
            newsprintField(
                value = geminiKey,
                onValueChange = onGeminiKeyChange,
                isError = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                onDone = onSaveGeminiKey,
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        }
        if (notificationTime != null && onNotificationTimeChange != null && onSaveNotificationTime != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = controlHeight),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "Notification time", style = typography.body.copy(color = NewsprintColors.ink))
                OutlinedTextField(
                    value = notificationTime,
                    onValueChange = onNotificationTimeChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = controlHeight),
                    singleLine = true,
                    isError = notificationTimeError != null,
                    textStyle = typography.body,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSaveNotificationTime() }),
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
                if (notificationTimeError != null) {
                    Text(
                        text = notificationTimeError,
                        style = typography.body.copy(color = NewsprintColors.error),
                    )
                }
            }
        }
        if (notificationBody != null && onNotificationBodyChange != null && onSaveNotificationBody != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = controlHeight),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "Notification text", style = typography.body.copy(color = NewsprintColors.ink))
                OutlinedTextField(
                    value = notificationBody,
                    onValueChange = onNotificationBodyChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = controlHeight),
                    singleLine = false,
                    maxLines = 3,
                    textStyle = typography.body,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSaveNotificationBody() }),
                    shape = RoundedCornerShape(4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NewsprintColors.ink,
                        unfocusedBorderColor = NewsprintColors.ink,
                        focusedContainerColor = NewsprintColors.field,
                        unfocusedContainerColor = NewsprintColors.field,
                        cursorColor = NewsprintColors.ink,
                        focusedTextColor = NewsprintColors.ink,
                        unfocusedTextColor = NewsprintColors.ink,
                    ),
                )
            }
        }
        if (editionThemeId != null && onEditionThemeSelect != null) {
            PreferencePicker(
                label = "Theme",
                options = EDITION_THEME_OPTIONS,
                selectedId = editionThemeId,
                onSelect = onEditionThemeSelect,
                controlHeight = controlHeight,
            )
        }
        if (editionLayoutId != null && onEditionLayoutSelect != null) {
            PreferencePicker(
                label = "Layout",
                options = EDITION_LAYOUT_OPTIONS,
                selectedId = editionLayoutId,
                onSelect = onEditionLayoutSelect,
                controlHeight = controlHeight,
            )
        }
        if (showInterests) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = controlHeight)
                    .clickable(role = Role.Button, onClick = onOpenInterests),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "Interests", style = typography.body.copy(color = NewsprintColors.ink))
            }
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

@Composable
private fun PreferencePicker(
    label: String,
    options: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit,
    controlHeight: androidx.compose.ui.unit.Dp,
) {
    val editionTypography = LocalMatutineEditionTypography.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = label, style = editionTypography.body.copy(color = NewsprintColors.ink))
        options.forEach { (id, title) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = controlHeight)
                    .clickable(role = Role.RadioButton, onClick = { onSelect(id) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = title, style = editionTypography.body.copy(color = NewsprintColors.ink))
                Text(
                    text = if (id == selectedId) "Selected" else "",
                    style = editionTypography.meta.copy(color = NewsprintColors.inkSecondary),
                )
            }
            HorizontalDivider(thickness = 1.dp, color = NewsprintColors.rule)
        }
    }
}

@Composable
private fun newsprintField(
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
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
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
