package hu.toliver.matutine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.isTraySupported
import hu.toliver.matutine.store.desktopDataRoot

fun main() = application {
    var windowVisible by remember { mutableStateOf(true) }
    var trayOpenRequest by remember { mutableIntStateOf(0) }
    var trayGenerateRequest by remember { mutableIntStateOf(0) }
    val trayIcon = rememberMatutineTrayIcon()
    val windowFlush = remember { WindowFlush() }

    if (isTraySupported) {
        Tray(
            icon = trayIcon,
            tooltip = "Matutine",
            menu = {
                Item(
                    text = "Open",
                    onClick = {
                        trayOpenRequest++
                        windowVisible = true
                    },
                )
                Item(
                    text = "Generate today's edition",
                    onClick = {
                        trayGenerateRequest++
                        windowVisible = true
                    },
                )
                Item(
                    text = "Quit",
                    onClick = ::exitApplication,
                )
            },
        )
    }

    Window(
        onCloseRequest = {
            val hideOrLeave = {
                if (isTraySupported) {
                    windowVisible = false
                } else {
                    exitApplication()
                }
            }
            val save = windowFlush.saveOpenTypewriter
            if (save == null) hideOrLeave() else save(hideOrLeave)
        },
        visible = windowVisible,
        title = "Matutine",
    ) {
        MatutineApp(
            dataRoot = desktopDataRoot(),
            trayOpenRequest = trayOpenRequest,
            trayGenerateRequest = trayGenerateRequest,
            windowFlush = windowFlush,
        ) { pane ->
            TypewriterScreen(pane)
        }
    }
}
