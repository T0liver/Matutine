package hu.toliver.matutine.edition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GeminiBriefingTest {
    private val stories = listOf(
        EditionStory(
            headline = "Markets open higher",
            sourceName = "Personal Wire",
            deck = "Traders watched the open.",
            sourceUrl = "https://example.com/lead",
            rank = StoryRank.lead,
        ),
    )

    @Test
    fun promptAndBodyStayOnTheFreeTierAndLeaveTheKeyOut() {
        val prompt = geminiBriefingPrompt(stories)
        val body = geminiGenerateBody(prompt)

        assertTrue(prompt.contains("Markets open higher"))
        assertTrue(prompt.contains("Traders watched the open."))
        assertFalse(prompt.contains("Summary assembled from headlines."))
        assertFalse(body.contains("configured"))
        assertFalse(GEMINI_GENERATE_URL.contains("key="))
        assertTrue(GEMINI_GENERATE_URL.contains("gemini-2.5-flash"))
        assertFalse(GEMINI_GENERATE_URL.contains("pro"))
    }

    @Test
    fun responseTextJoinsPartsAndUnescapes() {
        val body = """{"candidates":[{"content":{"parts":[{"text":"The harbor is quiet."},{"text":" Ships\nmove."}]}}]}"""

        assertEquals("The harbor is quiet. Ships\nmove.", briefingTextFromGeminiResponse(body))
        assertTrue(runCatching { briefingTextFromGeminiResponse("""{"candidates":[]}""") }.isFailure)
    }
}
