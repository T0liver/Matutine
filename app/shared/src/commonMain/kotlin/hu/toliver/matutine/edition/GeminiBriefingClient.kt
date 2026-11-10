package hu.toliver.matutine.edition

expect suspend fun postGeminiGenerateContent(apiKey: String, body: String): String
