package hu.toliver.matutine.edition

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI

actual fun openRemoteTransport(baseUrl: String): RemoteEditionTransport =
    DesktopRemoteTransport(baseUrl)

private class DesktopRemoteTransport(
    private val baseUrl: String,
) : RemoteEditionTransport {
    override suspend fun exchange(
        method: String,
        path: String,
        token: String,
        body: String?,
    ): RemoteHttpResult = withContext(Dispatchers.IO) {
        val connection = (URI.create(baseUrl.trimEnd('/') + path).toURL().openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 15_000
            readTimeout = 20_000
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                connection.outputStream.use { stream -> stream.write(body.encodeToByteArray()) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { reader -> reader.readText() }.orEmpty()
            RemoteHttpResult(code, text)
        } finally {
            connection.disconnect()
        }
    }
}
