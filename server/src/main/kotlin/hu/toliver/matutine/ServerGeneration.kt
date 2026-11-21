package hu.toliver.matutine

import hu.toliver.matutine.edition.EditionStore
import hu.toliver.matutine.edition.EditionStory
import hu.toliver.matutine.edition.FEED_DIDNT_LOAD
import hu.toliver.matutine.edition.GEMINI_GENERATE_URL
import hu.toliver.matutine.edition.LocalEditionRepository
import hu.toliver.matutine.edition.ReaderSettings
import hu.toliver.matutine.edition.briefingTextFromGeminiResponse
import hu.toliver.matutine.edition.geminiBriefingPrompt
import hu.toliver.matutine.edition.geminiGenerateBody
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.Charset
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal fun serverEditionRepository(
    documents: EditionStore,
    zone: TimeZone,
    clock: () -> Instant,
    geminiKey: String,
    readFeed: suspend (String) -> String,
    readCalDav: suspend (hu.toliver.matutine.edition.CalDavAccount) -> String = ::serverReadCalDav,
    summarize: suspend (List<EditionStory>, String) -> String,
): LocalEditionRepository {
    val trimmed = geminiKey.trim()
    val store = if (trimmed.isEmpty()) documents else GeminiKeyOverlay(documents, trimmed)
    return LocalEditionRepository(
        store = store,
        today = { clock().toLocalDateTime(zone).date },
        timeZone = zone,
        readFeed = readFeed,
        readCalDav = readCalDav,
        summarize = summarize,
    )
}

/**
 * The env key is visible to the generator and is never written back.
 * A settings file that was already there keeps its own key.
 */
private class GeminiKeyOverlay(
    private val base: EditionStore,
    private val key: String,
) : EditionStore by base {
    override fun readSettings(): ReaderSettings? {
        val current = base.readSettings() ?: ReaderSettings()
        return current.copy(geminiKey = key)
    }

    override fun writeSettings(settings: ReaderSettings) {
        val stored = base.readSettings()?.geminiKey.orEmpty()
        base.writeSettings(settings.copy(geminiKey = stored))
    }
}

internal suspend fun serverReadFeed(url: String): String = withContext(Dispatchers.IO) {
    val connection = (URI.create(url).toURL().openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = true
        connectTimeout = 20_000
        readTimeout = 20_000
        requestMethod = "GET"
        setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
        setRequestProperty("User-Agent", "Matutine")
    }
    try {
        if (connection.responseCode !in 200..299) error(FEED_DIDNT_LOAD)
        val charset = charsetFrom(connection.contentType)
        connection.inputStream.bufferedReader(charset).use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

internal suspend fun serverSummarize(stories: List<EditionStory>, apiKey: String): String =
    withContext(Dispatchers.IO) {
        val body = geminiGenerateBody(geminiBriefingPrompt(stories))
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
            briefingTextFromGeminiResponse(connection.inputStream.bufferedReader().use { it.readText() })
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
