package hu.toliver.matutine.edition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import hu.toliver.matutine.theme.EditionThemeColors

@Composable
fun LightEditLine(
    text: String,
    style: TextStyle,
    resetKey: Any,
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
    editable: Boolean = true,
    singleLine: Boolean = true,
) {
    if (!editable) {
        Text(text = text, style = style, modifier = modifier)
        return
    }
    var active by remember { mutableStateOf(false) }
    var draft by remember(text, resetKey) { mutableStateOf(text) }
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    val ink = EditionThemeColors.current.ink

    LaunchedEffect(resetKey) {
        draft = text
        active = false
    }

    LaunchedEffect(active) {
        if (active) focusRequester.requestFocus()
    }

    if (!active) {
        Text(
            text = draft,
            style = style,
            modifier = modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    draft = text
                    active = true
                },
        )
        return
    }

    BasicTextField(
        value = draft,
        onValueChange = { draft = it },
        textStyle = style.copy(color = ink, textDecoration = TextDecoration.None),
        cursorBrush = SolidColor(ink),
        singleLine = singleLine,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { focus ->
                if (!focus.isFocused) {
                    active = false
                    if (draft != text) onCommit(draft)
                }
            }
            .drawBehind {
                val underline = with(density) { 1.dp.toPx() }
                val y = size.height
                drawLine(
                    color = ink,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = underline,
                )
            },
        decorationBox = { inner ->
            Box { inner() }
        },
    )
}
