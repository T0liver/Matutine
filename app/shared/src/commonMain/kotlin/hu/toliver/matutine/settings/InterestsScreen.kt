package hu.toliver.matutine.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hu.toliver.matutine.edition.EditionCopy
import hu.toliver.matutine.edition.chromeControlMinHeight
import hu.toliver.matutine.edition.topicsFromInterests
import hu.toliver.matutine.theme.LocalMatutineEditionTypography
import hu.toliver.matutine.theme.NewsprintColors

internal val InterestChoices: List<String> = listOf(
    "Video games",
    "Technology",
    "Biology",
    "Finance",
    "Science",
    "Sports",
    "World news",
    "Culture",
    "Health",
    "Local",
)

internal const val ADD_ANOTHER = "Add another"
internal const val SAVE_TOPICS = "Save"

internal data class InterestDraft(
    val selected: List<String>,
    val freeLine: String,
    val dayLines: List<String>,
)

internal fun draftFromTopics(topics: List<String>): InterestDraft {
    val known = InterestChoices.toSet()
    val selected = mutableListOf<String>()
    val custom = mutableListOf<String>()
    topics.forEach { topic ->
        if (topic in known && topic !in selected) selected += topic
        else custom += topic
    }
    val dayLines = custom.drop(1)
    return InterestDraft(
        selected = selected,
        freeLine = custom.firstOrNull().orEmpty(),
        dayLines = dayLines.ifEmpty { listOf("") },
    )
}

@Composable
fun InterestsScreen(
    topics: List<String>,
    onSave: (List<String>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val typography = LocalMatutineEditionTypography.current
    val controlHeight = chromeControlMinHeight()
    val draft = remember(topics) { draftFromTopics(topics) }
    var selected by remember(topics) { mutableStateOf(draft.selected) }
    var freeLine by remember(topics) { mutableStateOf(draft.freeLine) }
    var dayLines by remember(topics) { mutableStateOf(draft.dayLines) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NewsprintColors.chrome)
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Interests", style = typography.story.copy(color = NewsprintColors.ink))
        Text(
            text = EditionCopy.WHAT_DO_YOU_WANT_TO_READ_ABOUT,
            style = typography.body.copy(color = NewsprintColors.ink),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InterestChoices.forEach { choice ->
                val on = choice in selected
                val shape = RoundedCornerShape(4.dp)
                Box(
                    modifier = Modifier
                        .height(controlHeight)
                        .clip(shape)
                        .background(if (on) NewsprintColors.ink else NewsprintColors.field)
                        .then(
                            if (on) {
                                Modifier
                            } else {
                                Modifier.border(1.dp, NewsprintColors.ink, shape)
                            },
                        )
                        .toggleable(
                            value = on,
                            role = Role.Checkbox,
                            onValueChange = { checked ->
                                selected = if (checked) selected + choice else selected - choice
                            },
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = choice,
                        style = typography.body.copy(
                            color = if (on) NewsprintColors.paper else NewsprintColors.ink,
                        ),
                    )
                }
            }
        }
        TopicLine(
            value = freeLine,
            onValueChange = { freeLine = it },
            controlHeight = controlHeight,
        )
        Text(
            text = EditionCopy.WHAT_SHOULD_THE_PAGE_NOTICE,
            style = typography.body.copy(color = NewsprintColors.ink),
        )
        dayLines.forEachIndexed { index, line ->
            TopicLine(
                value = line,
                onValueChange = { updated ->
                    dayLines = dayLines.toMutableList().also { it[index] = updated }
                },
                controlHeight = controlHeight,
            )
        }
        OutlinedButton(
            onClick = { dayLines = dayLines + "" },
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
                text = ADD_ANOTHER,
                style = typography.body.copy(color = NewsprintColors.ink),
            )
        }
        Button(
            onClick = {
                onSave(
                    topicsFromInterests(
                        selectedChoices = selected,
                        freeLine = freeLine,
                        dayLines = dayLines,
                    ),
                )
            },
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
                text = SAVE_TOPICS,
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
                text = "Back",
                style = typography.body.copy(color = NewsprintColors.ink),
            )
        }
    }
}

@Composable
private fun TopicLine(
    value: String,
    onValueChange: (String) -> Unit,
    controlHeight: Dp,
) {
    val typography = LocalMatutineEditionTypography.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = controlHeight),
        singleLine = true,
        textStyle = typography.body,
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
