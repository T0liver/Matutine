package hu.toliver.matutine.edition

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.Charset

actual suspend fun readFeedDocument(url: String): String = withContext(Dispatchers.IO) {
    val connection = (URI.create(url).toURL().openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = true
        connectTimeout = 20_000
        readTimeout = 20_000
        requestMethod = "GET"
        setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
        setRequestProperty("User-Agent", "Matutine")
    }
    try {
        val code = connection.responseCode
        if (code !in 200..299) error(FEED_DIDNT_LOAD)
        val charset = charsetFrom(connection.contentType)
        connection.inputStream.bufferedReader(charset).use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

private fun charsetFrom(contentType: String?): Charset {
    val name = contentType
        ?.let { Regex("charset=([^;]+)", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
        ?.trim()
        ?.trim('"')
    if (name.isNullOrBlank()) return Charsets.UTF_8
    return runCatching { Charset.forName(name) }.getOrDefault(Charsets.UTF_8)
}
