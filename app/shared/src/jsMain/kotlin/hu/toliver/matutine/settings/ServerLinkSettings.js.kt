package hu.toliver.matutine.settings

import hu.toliver.matutine.edition.ServerLink

actual fun showsServerLinkSettings(): Boolean = false

actual fun readDeviceServerLink(dataRoot: String): ServerLink? = dataRoot.let { null }

actual fun writeDeviceServerLink(dataRoot: String, link: ServerLink) {
    if (dataRoot.isEmpty() && link.baseUrl.isEmpty()) return
}

actual fun clearDeviceServerLink(dataRoot: String) {
    if (dataRoot.isEmpty()) return
}
