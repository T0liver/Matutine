package hu.toliver.matutine

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import hu.toliver.matutine.store.desktopDataRoot

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Matutine",
    ) {
        MatutineApp(dataRoot = desktopDataRoot())
    }
}