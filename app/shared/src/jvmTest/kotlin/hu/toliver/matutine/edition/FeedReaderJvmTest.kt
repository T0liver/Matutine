package hu.toliver.matutine.edition

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedReaderJvmTest {
    @Test
    fun http200ReturnsTheBodyAndNon2xxFails() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/ok") { exchange ->
            val body = "<rss>ok</rss>".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
            exchange.close()
        }
        server.createContext("/missing") { exchange ->
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
        }
        server.start()
        try {
            val port = server.address.port
            assertEquals("<rss>ok</rss>", readFeedDocument("http://127.0.0.1:$port/ok"))
            val failure = runCatching { readFeedDocument("http://127.0.0.1:$port/missing") }
            assertTrue(failure.isFailure)
            assertEquals(FEED_DIDNT_LOAD, failure.exceptionOrNull()?.message)
        } finally {
            server.stop(0)
        }
    }
}
