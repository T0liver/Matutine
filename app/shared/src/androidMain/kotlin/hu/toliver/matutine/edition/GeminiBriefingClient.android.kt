package hu.toliver.matutine.edition

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI

actual suspend fun postGeminiGenerateContent(apiKey: String, body: String): String = withContext(Dispatchers.IO) {
    val connection = (URI.create(GEMINI_GENERATE_URL).toURL().openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = false
        connectTimeout = 20_000
        readTimeout = 20_000
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        setRequestProperty("Accept", "application/json")
        setRequestProperty("x-goog-api-key", apiKey)
    }
    try {
        connection.outputStream.use { it.write(body.encodeToByteArray()) }
        if (connection.responseCode !in 200..299) error("summarizer_unavailable")
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}
