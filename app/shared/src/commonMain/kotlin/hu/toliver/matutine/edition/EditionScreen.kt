package hu.toliver.matutine.edition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import hu.toliver.matutine.browser.openUrl
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
    modifier: Modifier = Modifier,
) {
    val typography = LocalMatutineEditionTypography.current
    when (state) {
        EditionUiState.Loading -> LoadingEdition(modifier)
        is EditionUiState.SampleLoadFailed -> SampleLoadError(onRetrySample, typography, modifier)
        is EditionUiState.OtherError -> OtherError(state.message, typography, modifier)
        is EditionUiState.Ready -> EditionContent(
            edition = state.edition,
            generating = state.generating,
            notice = state.notice,
            showNoTopicLine = showNoTopicLine,
            onOpenInterests = onOpenInterests,
            onOpenSettings = onOpenSettings,
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
            .background(NewsprintColors.paper)
            .safeContentPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = NewsprintColors.ink)
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
            style = typography.body.copy(color = NewsprintColors.ink),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = NewsprintColors.ink,
                contentColor = NewsprintColors.paper,
            ),
            shape = RoundedCornerShape(4.dp),
        ) {
            Text(text = "Try again", style = typography.body.copy(color = NewsprintColors.paper))
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
private fun EditionContent(
    edition: Edition,
    generating: Boolean,
    notice: String?,
    showNoTopicLine: Boolean,
    onOpenInterests: () -> Unit,
    onOpenSettings: () -> Unit,
    typography: MatutineEditionTypography,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val forceSingleColumn = editionForceSingleColumn()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(NewsprintColors.paper)
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
            TextButton(
                onClick = onOpenSettings,
                modifier = Modifier.heightIn(min = chromeControlMinHeight()),
            ) {
                Text(text = "Settings", style = typography.body.copy(color = NewsprintColors.ink))
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
                    style = typography.body.copy(color = NewsprintColors.error),
                )
            }
            edition.sourceNotes.forEach { note ->
                Text(
                    text = note,
                    style = typography.body.copy(color = NewsprintColors.error),
                )
            }
            if (edition.isSample) {
                Text(
                    text = "SAMPLE EDITION",
                    style = typography.kicker.copy(color = NewsprintColors.accent),
                )
            } else if (showNoTopicLine) {
                TextButton(
                    onClick = onOpenInterests,
                    modifier = Modifier.heightIn(min = chromeControlMinHeight()),
                    colors = ButtonDefaults.textButtonColors(contentColor = NewsprintColors.ink),
                ) {
                    Text(
                        text = EditionCopy.WHAT_DO_YOU_WANT_TO_READ_ABOUT,
                        style = typography.body.copy(color = NewsprintColors.ink),
                    )
                }
            }
            if (wide) {
                WideEditionBody(edition, typography)
            } else {
                Spacer(Modifier.height(8.dp))
                BriefingBlock(edition.briefing, typography)
                StoryBlock(edition.stories, typography)
                CalendarBlock(edition.events, typography)
                TaskBlock(edition.tasks, typography)
            }
        }
    }
}

@Composable
private fun WideEditionBody(edition: Edition, typography: MatutineEditionTypography) {
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
            color = NewsprintColors.rule,
        )
        Column(
            modifier = Modifier.width(EditionRailWidth),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CalendarBlock(edition.events, typography)
            TaskBlock(edition.tasks, typography)
        }
    }
}

@Composable
private fun Masthead(name: String, dateLine: String, typography: MatutineEditionTypography) {
    val density = LocalDensity.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = name,
            style = typography.masthead,
            modifier = Modifier.drawBehind {
                val accentThickness = with(density) { 2.dp.toPx() }
                val y = size.height
                drawLine(
                    color = NewsprintColors.accent,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = accentThickness,
                )
            },
        )
        Text(
            text = dateLine,
            style = typography.meta,
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = NewsprintColors.ink,
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
private fun CalendarBlock(events: List<EditionEvent>, typography: MatutineEditionTypography) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Calendar", typography)
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
private fun TaskBlock(tasks: List<EditionTask>, typography: MatutineEditionTypography) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("Tasks", typography)
        if (tasks.isEmpty()) {
            Text(text = EditionCopy.NO_TASKS, style = typography.body)
        } else {
            tasks.forEach { task ->
                TaskRow(task, typography)
            }
        }
    }
}

@Composable
private fun TaskRow(task: EditionTask, typography: MatutineEditionTypography) {
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .border(1.dp, NewsprintColors.ink, RoundedCornerShape(0.dp)),
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
    HorizontalDivider(thickness = 1.dp, color = NewsprintColors.rule)
}
