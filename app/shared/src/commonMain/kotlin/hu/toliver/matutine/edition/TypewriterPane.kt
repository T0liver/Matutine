package hu.toliver.matutine.edition

class TypewriterPane(
    val text: String,
    val saved: Boolean,
    val error: String?,
    val onTextChange: (String) -> Unit,
    val onPause: (String) -> Unit,
    val onDiscard: () -> Unit,
    val onEscape: () -> Unit,
)
