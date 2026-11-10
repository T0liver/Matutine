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
            typography = typography,
            modifier = modifier,
        )
    }
}

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
        TextButton(
            onClick = onOpenSettings,
            modifier = Modifier.heightIn(min = chromeControlMinHeight()),
        ) {
            Text(text = "Settings", style = typography.body.copy(color = EditionThemeColors.current.ink))
        }
        Masthead(name = "Matutine", dateLine = "", typography = typography)
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
            }
            Masthead(edition.mastheadName, edition.dateLine, typography)
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
                    text = "SAMPLE EDITION",
                    style = typography.kicker.copy(color = EditionThemeColors.current.accent),
                )
            } else if (showNoTopicLine) {
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
                    typography = typography,
                )
            } else {
                Spacer(Modifier.height(8.dp))
                BriefingBlock(edition.briefing, typography)
                StoryBlock(edition.stories, typography)
                CalendarBlock(edition.events, calendarPermissionOff, onOpenCalendarPermission, typography)
                TaskBlock(
                    tasks = edition.tasks,
                    taskSaved = taskSaved,
                    onToggleTask = onToggleTask,
                    onAddTask = onAddTask,
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
            BriefingBlock(edition.briefing, typography)
            StoryBlockTwoColumns(edition.stories, typography)
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
            CalendarBlock(edition.events, calendarPermissionOff, onOpenCalendarPermission, typography)
            TaskBlock(
                tasks = edition.tasks,
                taskSaved = taskSaved,
                onToggleTask = onToggleTask,
                onAddTask = onAddTask,
                typography = typography,
            )
        }
    }
}

@Composable
private fun Masthead(name: String, dateLine: String, typography: MatutineEditionTypography) {
    val density = LocalDensity.current
    val accent = EditionThemeColors.current.accent
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = name,
            style = typography.masthead,
            modifier = Modifier.drawBehind {
                val accentThickness = with(density) { 2.dp.toPx() }
                val y = size.height
                drawLine(
                    color = accent,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = accentThickness,
                )
            },
        )
        if (dateLine.isNotBlank()) {
            Text(
                text = dateLine,
                style = typography.meta,
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = EditionThemeColors.current.ink,
        )
    }
}

@Composable
private fun BriefingBlock(briefing: String, typography: MatutineEditionTypography) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Briefing", typography)
        Text(text = briefing, style = typography.briefing)
    }
}

@Composable
private fun StoryBlock(stories: List<EditionStory>, typography: MatutineEditionTypography) {
    if (stories.isEmpty()) {
        Text(text = EditionCopy.NO_STORIES, style = typography.body)
        return
    }
    StoryColumn(stories, typography)
}

@Composable
private fun StoryBlockTwoColumns(stories: List<EditionStory>, typography: MatutineEditionTypography) {
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
            StoryColumn(left, typography)
        }
        Column(modifier = Modifier.weight(1f)) {
            StoryColumn(right, typography)
        }
    }
}

@Composable
private fun StoryColumn(stories: List<EditionStory>, typography: MatutineEditionTypography) {
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
            StoryRow(story, typography)
        }
    }
}

@Composable
private fun StoryRow(story: EditionStory, typography: MatutineEditionTypography) {
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(touchTarget)
            .clickable { openUrl(story.sourceUrl) }
            .semantics { contentDescription = accessible },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = story.headline, style = headlineStyle)
        Text(
            text = story.sourceName,
            style = typography.meta,
        )
        Text(text = story.deck, style = typography.body)
    }
}

@Composable
private fun CalendarBlock(
    events: List<EditionEvent>,
    calendarPermissionOff: Boolean,
    onOpenCalendarPermission: () -> Unit,
    typography: MatutineEditionTypography,
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
                    Text(text = event.title, style = typography.body)
                    Text(text = event.timeLabel, style = typography.meta)
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
    typography: MatutineEditionTypography,
) {
    var draft by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Tasks", typography)
        if (tasks.isEmpty()) {
            Text(text = EditionCopy.NO_TASKS, style = typography.body)
        } else {
            tasks.forEachIndexed { index, task ->
                TaskRow(task, typography, onToggle = { onToggleTask(index) })
            }
        }
        TaskAddLine(
            value = draft,
            onValueChange = { draft = it },
            onConfirm = {
                onAddTask(draft)
                draft = ""
            },
            typography = typography,
        )
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
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
