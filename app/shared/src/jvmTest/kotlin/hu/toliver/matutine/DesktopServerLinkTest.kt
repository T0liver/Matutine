package hu.toliver.matutine

import com.sun.net.httpserver.HttpServer
import hu.toliver.matutine.edition.openRemoteTransport
import hu.toliver.matutine.edition.ServerLink
import hu.toliver.matutine.settings.clearDeviceServerLink
import hu.toliver.matutine.settings.phoneSettingsVisibility
import hu.toliver.matutine.settings.readDeviceServerLink
import hu.toliver.matutine.settings.showsAndroidPaperCustomization
import hu.toliver.matutine.settings.showsNotificationTimeRow
import hu.toliver.matutine.settings.showsServerLinkSettings
import hu.toliver.matutine.settings.writeDeviceServerLink
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class DesktopServerLinkTest {
    @Test
    fun savingAServerLinkDoesNotTouchLocalDocuments() {
        val root = Files.createTempDirectory("matutine-desktop-link").toFile()
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
    fun remoteDesktopSettingsShowTheLinkAndNotPaperCustomization() {
        assertTrue(showsServerLinkSettings())
        assertFalse(showsAndroidPaperCustomization())
        assertFalse(showsNotificationTimeRow())
        val remote = phoneSettingsVisibility(serverConfigured = true)
        assertTrue(remote.offerForgetServer)
        assertFalse(remote.showSources)
        assertFalse(remote.showGeminiKey)
        assertFalse(remote.showInterests)
        val local = phoneSettingsVisibility(serverConfigured = false)
        assertFalse(local.offerForgetServer)
        assertTrue(local.showSources)
    }

    @Test
    fun desktopTransportSendsTheBearerToken() = runBlocking {
        var authorization: String? = null
        var method: String? = null
        var body = ""
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/editions/current") { exchange ->
            authorization = exchange.requestHeaders.getFirst("Authorization")
            method = exchange.requestMethod
            body = exchange.requestBody.bufferedReader().use { it.readText() }
            val bytes = "paper".encodeToByteArray()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            val port = server.address.port
            val result = openRemoteTransport("http://127.0.0.1:$port").exchange(
                method = "GET",
                path = "/editions/current",
                token = "lan-secret",
            )
            assertEquals(200, result.status)
            assertEquals("paper", result.body)
            assertEquals("GET", method)
            assertEquals("Bearer lan-secret", authorization)
            assertEquals("", body)
        } finally {
            server.stop(0)
        }
    }
}
