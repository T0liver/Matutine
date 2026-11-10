package hu.toliver.matutine.settings

import hu.toliver.matutine.edition.ServerLink
import hu.toliver.matutine.edition.decodeServerLink
import hu.toliver.matutine.edition.encodeServerLink
import java.io.File

actual fun showsServerLinkSettings(): Boolean = true

actual fun readDeviceServerLink(dataRoot: String): ServerLink? =
    FileServerLinkStore(dataRoot).read()

actual fun writeDeviceServerLink(dataRoot: String, link: ServerLink) {
    FileServerLinkStore(dataRoot).write(link)
}

actual fun clearDeviceServerLink(dataRoot: String) {
    FileServerLinkStore(dataRoot).clear()
}

internal class FileServerLinkStore(
    rootPath: String,
) {
    private val file = File(rootPath, "server-link.json")

    fun read(): ServerLink? {
        if (!file.exists()) return null
        return runCatching { decodeServerLink(file.readText()) }.getOrNull()
    }

    fun write(link: ServerLink) {
        file.parentFile?.mkdirs()
        file.writeText(encodeServerLink(link))
    }

    fun clear() {
        if (file.exists()) file.delete()
    }
}
