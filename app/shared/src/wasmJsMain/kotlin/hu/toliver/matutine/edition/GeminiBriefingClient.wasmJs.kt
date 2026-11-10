package hu.toliver.matutine.edition

actual suspend fun postGeminiGenerateContent(
    @Suppress("UNUSED_PARAMETER") apiKey: String,
    @Suppress("UNUSED_PARAMETER") body: String,
): String = error("summarizer_unavailable")
