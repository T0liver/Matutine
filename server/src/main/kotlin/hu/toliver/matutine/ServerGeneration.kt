package hu.toliver.matutine

import hu.toliver.matutine.edition.EditionStore
import hu.toliver.matutine.edition.EditionStory
import hu.toliver.matutine.edition.FEED_DIDNT_LOAD
import hu.toliver.matutine.edition.LocalEditionRepository
import hu.toliver.matutine.edition.ReaderSettings
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.Charset
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val GEMINI_GENERATE_URL =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

internal fun serverEditionRepository(
    documents: EditionStore,
    zone: TimeZone,
    clock: () -> Instant,
    geminiKey: String,
    readFeed: suspend (String) -> String,
    summarize: suspend (List<EditionStory>, String) -> String,
): LocalEditionRepository {
    val trimmed = geminiKey.trim()
    val store = if (trimmed.isEmpty()) documents else GeminiKeyOverlay(documents, trimmed)
    return LocalEditionRepository(
        store = store,
        today = { clock().toLocalDateTime(zone).date },
        timeZone = zone,
        readFeed = readFeed,
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

private fun geminiBriefingPrompt(stories: List<EditionStory>): String {
    val lines = stories.joinToString("\n") { story ->
        val headline = story.headline.trim()
        val deck = story.deck.trim()
        if (deck.isEmpty()) headline else "$headline\n$deck"
    }
    return "Write a short newspaper briefing in plain prose from these headlines and decks. " +
        "No title, no model name, and no list.\n$lines"
}

private fun geminiGenerateBody(prompt: String): String =
    """{"contents":[{"parts":[{"text":${jsonString(prompt)}}]}]}"""

private fun briefingTextFromGeminiResponse(body: String): String {
    val text = Regex(""""text"\s*:\s*"((?:\\.|[^"\\])*)"""")
        .findAll(body)
        .map { unescapeJsonString(it.groupValues[1]) }
        .joinToString("")
        .trim()
    if (text.isEmpty()) error("empty_briefing")
    return text
}

private fun jsonString(value: String): String = buildString {
    append('"')
    value.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(char)
        }
    }
    append('"')
}

private fun unescapeJsonString(value: String): String = buildString {
    var index = 0
    while (index < value.length) {
        val char = value[index]
        if (char != '\\' || index + 1 >= value.length) {
            append(char)
            index += 1
            continue
        }
        when (val escaped = value[index + 1]) {
            '\\', '"', '/' -> {
                append(escaped)
                index += 2
            }
            'n' -> {
                append('\n')
                index += 2
            }
            'r' -> {
                append('\r')
                index += 2
            }
            't' -> {
                append('\t')
                index += 2
            }
            'u' -> {
                if (index + 5 >= value.length) error("empty_briefing")
                append(value.substring(index + 2, index + 6).toInt(16).toChar())
                index += 6
            }
            else -> error("empty_briefing")
        }
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
