package hu.toliver.matutine.edition

const val GEMINI_GENERATE_URL =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

fun geminiBriefingPrompt(stories: List<EditionStory>): String {
    val lines = stories.joinToString("\n") { story ->
        val headline = story.headline.trim()
        val deck = story.deck.trim()
        if (deck.isEmpty()) headline else "$headline\n$deck"
    }
    return "Write a short newspaper briefing in plain prose from these headlines and decks. " +
        "No title, no model name, and no list.\n$lines"
}

fun geminiGenerateBody(prompt: String): String =
    """{"contents":[{"parts":[{"text":${jsonString(prompt)}}]}]}"""

fun briefingTextFromGeminiResponse(body: String): String {
    val text = Regex(""""text"\s*:\s*"((?:\\.|[^"\\])*)"""")
        .findAll(body)
        .map { unescapeJsonString(it.groupValues[1]) }
        .joinToString("")
        .trim()
    if (text.isEmpty()) error("empty_briefing")
    return text
}

suspend fun summarizeWithGemini(stories: List<EditionStory>, apiKey: String): String {
    val response = postGeminiGenerateContent(apiKey, geminiGenerateBody(geminiBriefingPrompt(stories)))
    return briefingTextFromGeminiResponse(response)
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
