package hu.toliver.matutine.edition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import hu.toliver.matutine.browser.openUrl
import hu.toliver.matutine.theme.EditionThemeColors
import hu.toliver.matutine.theme.LocalMatutineEditionTypography
import hu.toliver.matutine.theme.MatutineEditionTypography
import hu.toliver.matutine.theme.NewsprintColors

@Composable
fun EditionScreen(
    state: EditionUiState,
    onRetrySample: () -> Unit,
    onOpenSettings: () -> Unit,
    showNoTopicLine: Boolean,
    onOpenInterests: () -> Unit,
    onToggleTask: (Int) -> Unit,
    onAddTask: (String) -> Unit,
    calendarPermissionOff: Boolean = false,
    onOpenCalendarPermission: () -> Unit = {},
    editionLayoutId: String = DEFAULT_EDITION_LAYOUT,
    onOpenTypewriter: (() -> Unit)? = null,
    sameDayExtras: List<Edition> = emptyList(),
    onNewEditionNow: () -> Unit = {},
    onOpenExtra: (String) -> Unit = {},
    onOpenMorning: () -> Unit = {},
    readOnly: Boolean = false,
    onOpenAdmin: (() -> Unit)? = null,
    lightEdit: LightEditHooks? = null,
    modifier: Modifier = Modifier,
) {
    val typography = LocalMatutineEditionTypography.current
    when (state) {
        EditionUiState.Loading -> LoadingEdition(modifier)
        is EditionUiState.SampleLoadFailed -> SampleLoadError(onRetrySample, typography, modifier)
        is EditionUiState.MissingMorning -> EmptyFrontPage(
            generating = state.generating,
            notice = state.notice,
            onOpenSettings = onOpenSettings,
            readOnly = readOnly,
            typography = typography,
            modifier = modifier,
        )
        is EditionUiState.OtherError -> OtherError(state.message, typography, modifier)
        is EditionUiState.Ready -> EditionContent(
            edition = state.edition,
            generating = state.generating,
            notice = state.notice,
            taskSaved = state.taskSaved,
            showNoTopicLine = showNoTopicLine,
            onOpenInterests = onOpenInterests,
            onOpenSettings = onOpenSettings,
            onToggleTask = onToggleTask,
            onAddTask = onAddTask,
            calendarPermissionOff = calendarPermissionOff,
            onOpenCalendarPermission = onOpenCalendarPermission,
            editionLayoutId = editionLayoutId,
            onOpenTypewriter = onOpenTypewriter,
            sameDayExtras = sameDayExtras,
            onNewEditionNow = onNewEditionNow,
            onOpenExtra = onOpenExtra,
            onOpenMorning = onOpenMorning,
            readOnly = readOnly,
            onOpenAdmin = onOpenAdmin,
            lightEdit = lightEdit,
            typography = typography,
            modifier = modifier,
        )
    }
}

data class LightEditHooks(
    val saved: Boolean,
    val error: String?,
    val resetKey: Int,
    val onCommit: (LightEditField, String) -> Unit,
    val onDiscard: () -> Unit,
)

@Composable
private fun LoadingEdition(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditionThemeColors.current.paper)
            .safeContentPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = EditionThemeColors.current.ink)
    }
}

@Composable
private fun SampleLoadError(
    onRetry: () -> Unit,
    typography: MatutineEditionTypography,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NewsprintColors.chrome)
            .safeContentPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "The sample edition didn't load.",
            style = typography.body.copy(color = EditionThemeColors.current.ink),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = EditionThemeColors.current.ink,
                contentColor = EditionThemeColors.current.paper,
            ),
            shape = RoundedCornerShape(4.dp),
        ) {
            Text(text = "Try again", style = typography.body.copy(color = EditionThemeColors.current.paper))
        }
    }
}

@Composable
private fun OtherError(message: String, typography: MatutineEditionTypography, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NewsprintColors.chrome)
            .safeContentPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, style = typography.body)
    }
}

@Composable
private fun EmptyFrontPage(
    generating: Boolean,
    notice: String?,
    onOpenSettings: () -> Unit,
    readOnly: Boolean,
    typography: MatutineEditionTypography,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditionThemeColors.current.paper)
            .safeContentPadding()
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!readOnly) {
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.heightIn(min = chromeControlMinHeight()),
            ) {
                Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
            }
        }
        Masthead(name = "Matutine", dateLine = "", typography = typography, lightEdit = null)
        if (generating) {
            Text(
                text = EditionCopy.SETTING_TODAYS_EDITION,
                style = typography.body,
            )
        }
        if (!notice.isNullOrBlank()) {
            Text(
                text = notice,
                style = typography.body.copy(color = EditionThemeColors.current.error),
            )
        }
    }
}

@Composable
private fun EditionContent(
    edition: Edition,
    generating: Boolean,
    notice: String?,
    taskSaved: Boolean,
    showNoTopicLine: Boolean,
    onOpenInterests: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleTask: (Int) -> Unit,
    onAddTask: (String) -> Unit,
    calendarPermissionOff: Boolean,
    onOpenCalendarPermission: () -> Unit,
    editionLayoutId: String,
    onOpenTypewriter: (() -> Unit)?,
    sameDayExtras: List<Edition>,
    onNewEditionNow: () -> Unit,
    onOpenExtra: (String) -> Unit,
    onOpenMorning: () -> Unit,
    readOnly: Boolean,
    onOpenAdmin: (() -> Unit)?,
    lightEdit: LightEditHooks?,
    typography: MatutineEditionTypography,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val forceSingleColumn = editionForceSingleColumnForLayout(editionLayoutId)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(EditionThemeColors.current.paper)
            .safeContentPadding(),
    ) {
        val wide = usesWideEditionLayout(maxWidth, forceSingleColumn)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!readOnly) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.heightIn(min = chromeControlMinHeight()),
                    ) {
                        Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
                    }
                    if (onOpenTypewriter != null) {
                        TextButton(
                            onClick = onOpenTypewriter,
                            modifier = Modifier.heightIn(min = chromeControlMinHeight()),
                        ) {
                            Text(text = "Typewriter", style = typography.body.copy(color = EditionThemeColors.current.ink))
                        }
                    }
                    TextButton(
                        onClick = onNewEditionNow,
                        modifier = Modifier.heightIn(min = chromeControlMinHeight()),
                    ) {
                        Text(text = "New edition now", style = typography.body.copy(color = EditionThemeColors.current.ink))
                    }
                }
            } else if (onOpenAdmin != null) {
                TextButton(
                    onClick = onOpenAdmin,
                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
                ) {
                    Text(text = "Admin", style = typography.body.copy(color = EditionThemeColors.current.ink))
                }
            }
            if (edition.role == EditionRole.extra) {
                TextButton(
                    onClick = onOpenMorning,
                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
                ) {
                    Text(
                        text = "Today's morning edition",
                        style = typography.body.copy(color = EditionThemeColors.current.ink),
                    )
                }
            }
            sameDayExtras.forEach { extra ->
                TextButton(
                    onClick = { onOpenExtra(extra.id) },
                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
                ) {
                    Text(
                        text = sameDayExtraLabel(extra),
                        style = typography.body.copy(color = EditionThemeColors.current.ink),
                    )
                }
            }
            Masthead(edition.mastheadName, edition.dateLine, typography, lightEdit)
            if (lightEdit != null) {
                LightEditStatusRow(lightEdit, typography)
            }
            if (generating) {
                Text(
                    text = EditionCopy.SETTING_TODAYS_EDITION,
                    style = typography.body,
                )
            }
            if (!notice.isNullOrBlank()) {
                Text(
                    text = notice,
                    style = typography.body.copy(color = EditionThemeColors.current.error),
                )
            }
            edition.sourceNotes.forEach { note ->
                Text(
                    text = note,
                    style = typography.body.copy(color = EditionThemeColors.current.error),
                )
            }
            if (edition.isSample) {
                Text(
                    text = EditionTranscript.SAMPLE,
                    style = typography.kicker.copy(color = EditionThemeColors.current.accent),
                )
            } else if (showNoTopicLine && !readOnly) {
                TextButton(
                    onClick = onOpenInterests,
                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
                    colors = ButtonDefaults.textButtonColors(contentColor = EditionThemeColors.current.ink),
                ) {
                    Text(
                        text = EditionCopy.WHAT_DO_YOU_WANT_TO_READ_ABOUT,
                        style = typography.body.copy(color = EditionThemeColors.current.ink),
                    )
                }
            }
            if (wide) {
                WideEditionBody(
                    edition = edition,
                    taskSaved = taskSaved,
                    onToggleTask = onToggleTask,
                    onAddTask = onAddTask,
                    calendarPermissionOff = calendarPermissionOff,
                    onOpenCalendarPermission = onOpenCalendarPermission,
                    readOnly = readOnly,
                    lightEdit = lightEdit,
                    typography = typography,
                )
            } else {
                Spacer(Modifier.height(8.dp))
                BriefingBlock(edition.briefing, typography, lightEdit)
                StoryBlock(edition.stories, typography, lightEdit)
                CalendarBlock(edition.events, calendarPermissionOff, onOpenCalendarPermission, typography, lightEdit)
                TaskBlock(
                    tasks = edition.tasks,
                    taskSaved = taskSaved,
                    onToggleTask = onToggleTask,
                    onAddTask = onAddTask,
                    readOnly = readOnly,
                    typography = typography,
                )
            }
        }
    }
}

@Composable
private fun WideEditionBody(
    edition: Edition,
    taskSaved: Boolean,
    onToggleTask: (Int) -> Unit,
    onAddTask: (String) -> Unit,
    calendarPermissionOff: Boolean,
    onOpenCalendarPermission: () -> Unit,
    readOnly: Boolean,
    lightEdit: LightEditHooks?,
    typography: MatutineEditionTypography,
) {
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BriefingBlock(edition.briefing, typography, lightEdit)
            StoryBlockTwoColumns(edition.stories, typography, lightEdit)
        }
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = 1.dp,
            color = EditionThemeColors.current.rule,
        )
        Column(
            modifier = Modifier.width(EditionRailWidth),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CalendarBlock(edition.events, calendarPermissionOff, onOpenCalendarPermission, typography, lightEdit)
            TaskBlock(
                tasks = edition.tasks,
                taskSaved = taskSaved,
                onToggleTask = onToggleTask,
                onAddTask = onAddTask,
                readOnly = readOnly,
                typography = typography,
            )
        }
    }
}

@Composable
private fun LightEditStatusRow(lightEdit: LightEditHooks, typography: MatutineEditionTypography) {
    if (lightEdit.error != null) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = lightEdit.error,
                style = typography.body.copy(color = EditionThemeColors.current.error),
            )
            TextButton(
                onClick = lightEdit.onDiscard,
                modifier = Modifier.heightIn(min = chromeControlMinHeight()),
                colors = ButtonDefaults.textButtonColors(contentColor = EditionThemeColors.current.ink),
            ) {
                Text(text = "Discard", style = typography.body.copy(color = EditionThemeColors.current.ink))
            }
        }
    } else if (lightEdit.saved) {
        Text(text = EditionTranscript.SAVED, style = typography.meta)
    }
}

@Composable
private fun Masthead(
    name: String,
    dateLine: String,
    typography: MatutineEditionTypography,
    lightEdit: LightEditHooks?,
) {
    val density = LocalDensity.current
    val accent = EditionThemeColors.current.accent
    val resetKey = lightEdit?.resetKey ?: 0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val mastheadModifier = Modifier.drawBehind {
            val accentThickness = with(density) { 2.dp.toPx() }
            val y = size.height
            drawLine(
                color = accent,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = accentThickness,
            )
        }
        if (lightEdit != null) {
            LightEditLine(
                text = name,
                style = typography.masthead,
                resetKey = resetKey,
                modifier = mastheadModifier,
                onCommit = { lightEdit.onCommit(LightEditField.Masthead, it) },
            )
        } else {
            Text(
                text = name,
                style = typography.masthead,
                modifier = mastheadModifier,
            )
        }
        if (dateLine.isNotBlank() || lightEdit != null) {
            if (lightEdit != null) {
                LightEditLine(
                    text = dateLine,
                    style = typography.meta,
                    resetKey = resetKey,
                    onCommit = { lightEdit.onCommit(LightEditField.DateLine, it) },
                )
            } else {
                Text(
                    text = dateLine,
                    style = typography.meta,
                )
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = EditionThemeColors.current.ink,
        )
    }
}

@Composable
private fun BriefingBlock(briefing: String, typography: MatutineEditionTypography, lightEdit: LightEditHooks?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Briefing", typography)
        if (lightEdit != null) {
            LightEditLine(
                text = briefing,
                style = typography.briefing,
                resetKey = lightEdit.resetKey,
                singleLine = false,
                onCommit = { lightEdit.onCommit(LightEditField.Briefing, it) },
            )
        } else {
            Text(text = briefing, style = typography.briefing)
        }
    }
}

@Composable
private fun StoryBlock(
    stories: List<EditionStory>,
    typography: MatutineEditionTypography,
    lightEdit: LightEditHooks?,
) {
    if (stories.isEmpty()) {
        Text(text = EditionCopy.NO_STORIES, style = typography.body)
        return
    }
    StoryColumn(stories, typography, lightEdit)
}

@Composable
private fun StoryBlockTwoColumns(
    stories: List<EditionStory>,
    typography: MatutineEditionTypography,
    lightEdit: LightEditHooks?,
) {
    if (stories.isEmpty()) {
        Text(text = EditionCopy.NO_STORIES, style = typography.body)
        return
    }
    val (left, right) = storiesForTwoColumns(stories)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            StoryColumn(left, typography, lightEdit, storyOffset = 0)
        }
        Column(modifier = Modifier.weight(1f)) {
            StoryColumn(right, typography, lightEdit, storyOffset = left.size)
        }
    }
}

@Composable
private fun StoryColumn(
    stories: List<EditionStory>,
    typography: MatutineEditionTypography,
    lightEdit: LightEditHooks?,
    storyOffset: Int = 0,
) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        if (stories.isEmpty()) {
            return
        }
        stories.forEachIndexed { index, story ->
            if (index > 0) {
                Spacer(Modifier.height(12.dp))
                HairlineDivider()
                Spacer(Modifier.height(12.dp))
            }
            StoryRow(story, typography, lightEdit, storyOffset + index)
        }
    }
}

@Composable
private fun StoryRow(
    story: EditionStory,
    typography: MatutineEditionTypography,
    lightEdit: LightEditHooks?,
    storyIndex: Int,
) {
    val headlineStyle = when (story.rank) {
        StoryRank.lead -> typography.leadCompact
        StoryRank.secondary -> typography.story
        StoryRank.note -> typography.note
    }
    val accessible = "${story.sourceName}: ${story.headline}"
    val touchTarget = if (editionForceSingleColumn()) {
        Modifier.minimumInteractiveComponentSize()
    } else {
        Modifier
    }
    val openStory = lightEdit == null && story.sourceUrl.isNotBlank()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(touchTarget)
            .then(
                if (openStory) {
                    Modifier
                        .clickable { openUrl(story.sourceUrl) }
                        .semantics { contentDescription = accessible }
                } else {
                    Modifier
                },
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (lightEdit != null) {
            LightEditLine(
                text = story.headline,
                style = headlineStyle,
                resetKey = lightEdit.resetKey,
                onCommit = { lightEdit.onCommit(LightEditField.StoryHeadline(storyIndex), it) },
            )
            LightEditLine(
                text = story.sourceName,
                style = typography.meta,
                resetKey = lightEdit.resetKey,
                onCommit = { lightEdit.onCommit(LightEditField.StorySource(storyIndex), it) },
            )
            LightEditLine(
                text = story.deck,
                style = typography.body,
                resetKey = lightEdit.resetKey,
                singleLine = false,
                onCommit = { lightEdit.onCommit(LightEditField.StoryDeck(storyIndex), it) },
            )
        } else {
            Text(text = story.headline, style = headlineStyle)
            Text(
                text = story.sourceName,
                style = typography.meta,
            )
            Text(text = story.deck, style = typography.body)
        }
    }
}

@Composable
private fun CalendarBlock(
    events: List<EditionEvent>,
    calendarPermissionOff: Boolean,
    onOpenCalendarPermission: () -> Unit,
    typography: MatutineEditionTypography,
    lightEdit: LightEditHooks?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Calendar", typography)
        if (calendarPermissionOff) {
            TextButton(
                onClick = onOpenCalendarPermission,
                modifier = Modifier.heightIn(min = chromeControlMinHeight()),
                colors = ButtonDefaults.textButtonColors(contentColor = EditionThemeColors.current.ink),
            ) {
                Text(
                    text = EditionCopy.CALENDAR_PERMISSION_OFF,
                    style = typography.body.copy(color = EditionThemeColors.current.ink),
                )
            }
        }
        if (events.isEmpty()) {
            Text(text = EditionCopy.NO_EVENTS, style = typography.body)
        } else {
            events.forEachIndexed { index, event ->
                if (index > 0) {
                    Spacer(Modifier.height(8.dp))
                    HairlineDivider()
                    Spacer(Modifier.height(8.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (lightEdit != null) {
                        LightEditLine(
                            text = event.title,
                            style = typography.body,
                            resetKey = lightEdit.resetKey,
                            onCommit = { lightEdit.onCommit(LightEditField.EventTitle(index), it) },
                        )
                        LightEditLine(
                            text = event.timeLabel,
                            style = typography.meta,
                            resetKey = lightEdit.resetKey,
                            onCommit = { lightEdit.onCommit(LightEditField.EventTime(index), it) },
                        )
                    } else {
                        Text(text = event.title, style = typography.body)
                        Text(text = event.timeLabel, style = typography.meta)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskBlock(
    tasks: List<EditionTask>,
    taskSaved: Boolean,
    onToggleTask: (Int) -> Unit,
    onAddTask: (String) -> Unit,
    readOnly: Boolean,
    typography: MatutineEditionTypography,
) {
    var draft by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Tasks", typography)
        if (tasks.isEmpty()) {
            Text(text = EditionCopy.NO_TASKS, style = typography.body)
        } else {
            tasks.forEachIndexed { index, task ->
                TaskRow(
                    task,
                    typography,
                    onToggle = { onToggleTask(index) },
                    readOnly = readOnly,
                )
            }
        }
        if (!readOnly) {
            TaskAddLine(
                value = draft,
                onValueChange = { draft = it },
                onConfirm = {
                    onAddTask(draft)
                    draft = ""
                },
                typography = typography,
            )
        }
        if (taskSaved) {
            Text(text = EditionCopy.SAVED, style = typography.meta)
        }
    }
}

@Composable
private fun TaskAddLine(
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    typography: MatutineEditionTypography,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = chromeControlMinHeight()),
        textStyle = typography.body.copy(color = EditionThemeColors.current.ink),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onConfirm() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = EditionThemeColors.current.ink,
            unfocusedTextColor = EditionThemeColors.current.ink,
            focusedContainerColor = EditionThemeColors.current.paper,
            unfocusedContainerColor = EditionThemeColors.current.paper,
            focusedBorderColor = EditionThemeColors.current.ink,
            unfocusedBorderColor = EditionThemeColors.current.rule,
            cursorColor = EditionThemeColors.current.ink,
        ),
        shape = RoundedCornerShape(0.dp),
    )
}

@Composable
private fun TaskRow(
    task: EditionTask,
    typography: MatutineEditionTypography,
    onToggle: () -> Unit,
    readOnly: Boolean,
) {
    val touchTarget = if (editionForceSingleColumn()) {
        Modifier.minimumInteractiveComponentSize()
    } else {
        Modifier
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .padding(vertical = 4.dp)
            .then(touchTarget)
            .then(
                if (readOnly) {
                    Modifier
                } else {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle,
                    )
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(1.dp, EditionThemeColors.current.ink, RoundedCornerShape(0.dp))
                .then(
                    if (task.completed) {
                        Modifier.background(EditionThemeColors.current.ink)
                    } else {
                        Modifier
                    },
                ),
        )
        val titleStyle = if (task.completed) {
            typography.body.copy(textDecoration = TextDecoration.LineThrough)
        } else {
            typography.body
        }
        Text(text = task.title, style = titleStyle)
    }
}

@Composable
private fun SectionLabel(label: String, typography: MatutineEditionTypography) {
    Text(
        text = label.uppercase(),
        style = typography.kicker,
    )
}

@Composable
private fun HairlineDivider() {
    HorizontalDivider(thickness = 1.dp, color = EditionThemeColors.current.rule)
}
