package hu.toliver.matutine.edition

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val GEMINI_GENERATE_URL =
    "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent"

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
    """{"contents":[{"parts":[{"text":${JsonPrimitive(prompt)}}]}]}"""

fun briefingTextFromGeminiResponse(body: String): String {
    val root = editionJson.parseToJsonElement(body).jsonObject
    val parts = root["candidates"]
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonObject
        ?.get("content")
        ?.jsonObject
        ?.get("parts")
        ?.jsonArray
        ?: error("empty_briefing")
    val text = parts.mapNotNull { element ->
        val part = element.jsonObject
        if (part["thought"]?.jsonPrimitive?.booleanOrNull == true) return@mapNotNull null
        part["text"]?.jsonPrimitive?.contentOrNull
    }.joinToString("").trim()
    if (text.isEmpty()) error("empty_briefing")
    return text
}
