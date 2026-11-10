package hu.toliver.matutine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import hu.toliver.matutine.edition.EditionTranscript
import hu.toliver.matutine.edition.TypewriterPane
import hu.toliver.matutine.theme.EditionThemeColors
import hu.toliver.matutine.theme.LocalMatutineEditionTypography
import kotlinx.coroutines.delay

private const val TYPEWRITER_PAUSE_MILLIS = 1_000L

@Composable
fun TypewriterScreen(pane: TypewriterPane) {
    val colors = EditionThemeColors.current
    val typography = LocalMatutineEditionTypography.current
    val typewriter = typography.typewriter.copy(color = colors.ink)
    val focus = remember { FocusRequester() }
    var primed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    LaunchedEffect(pane.text) {
        if (!primed) {
            primed = true
            return@LaunchedEffect
        }
        delay(TYPEWRITER_PAUSE_MILLIS)
        pane.onPause(pane.text)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                    pane.onEscape()
                    true
                } else {
                    false
                }
            }
            .padding(16.dp),
    ) {
        BasicTextField(
            value = pane.text,
            onValueChange = pane.onTextChange,
            textStyle = typewriter,
            cursorBrush = SolidColor(colors.ink),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .focusRequester(focus)
                .semantics { contentDescription = "Edition" },
        )
        val error = pane.error
        if (!error.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            BasicText(text = error, style = typewriter.copy(color = colors.error))
        }
        if (pane.saved) {
            Spacer(Modifier.height(8.dp))
            BasicText(text = EditionTranscript.SAVED, style = typewriter)
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .height(40.dp)
                .border(1.dp, colors.ink, RoundedCornerShape(4.dp))
                .clickable(onClick = pane.onDiscard)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text = "Discard", style = typography.body.copy(color = colors.ink))
        }
    }
}
