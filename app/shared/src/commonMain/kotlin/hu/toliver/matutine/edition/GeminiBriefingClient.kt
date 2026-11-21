package hu.toliver.matutine.edition

expect suspend fun postGeminiGenerateContent(apiKey: String, body: String): String

suspend fun summarizeWithGemini(stories: List<EditionStory>, apiKey: String): String {
    val response = postGeminiGenerateContent(apiKey, geminiGenerateBody(geminiBriefingPrompt(stories)))
    return briefingTextFromGeminiResponse(response)
}
