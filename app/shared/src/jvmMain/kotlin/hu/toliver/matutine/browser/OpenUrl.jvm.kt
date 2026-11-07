package hu.toliver.matutine.browser

import java.awt.Desktop
import java.net.URI

actual fun openUrl(url: String) {
    if (!Desktop.isDesktopSupported()) return
    val desktop = Desktop.getDesktop()
    if (!desktop.isSupported(Desktop.Action.BROWSE)) return
    desktop.browse(URI(url))
}
