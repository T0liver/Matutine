package hu.toliver.matutine

import hu.toliver.matutine.edition.ServerLink
import hu.toliver.matutine.settings.clearDeviceServerLink
import hu.toliver.matutine.settings.readDeviceServerLink
import hu.toliver.matutine.settings.writeDeviceServerLink
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerLinkStoreTest {
    @Test
    fun savingAServerLinkDoesNotTouchLocalDocuments() {
        val root = Files.createTempDirectory("matutine-link").toFile()
        File(root, "sources.json").writeText("local-sources")
        File(root, "settings.json").writeText("local-settings")
        File(root, "calendar-snapshot.json").writeText("local-snapshot")
        File(root, "editions").mkdirs()
        File(root, "editions/2026-11-10-morning.json").writeText("local-edition")

        writeDeviceServerLink(root.absolutePath, ServerLink("http://192.168.0.5:8080", "lan-secret"))

        assertEquals("local-sources", File(root, "sources.json").readText())
        assertEquals("local-settings", File(root, "settings.json").readText())
        assertEquals("local-snapshot", File(root, "calendar-snapshot.json").readText())
        assertEquals("local-edition", File(root, "editions/2026-11-10-morning.json").readText())
        assertEquals(
            ServerLink("http://192.168.0.5:8080", "lan-secret"),
            readDeviceServerLink(root.absolutePath),
        )

        clearDeviceServerLink(root.absolutePath)

        assertNull(readDeviceServerLink(root.absolutePath))
        assertEquals("local-sources", File(root, "sources.json").readText())
        assertTrue(File(root, "editions/2026-11-10-morning.json").exists())
    }

    @Test
    fun manifestAllowsCleartextHttp() {
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertTrue(manifest.contains("android:usesCleartextTraffic=\"true\""))
        assertFalse(manifest.contains("networkSecurityConfig"))
    }
}
