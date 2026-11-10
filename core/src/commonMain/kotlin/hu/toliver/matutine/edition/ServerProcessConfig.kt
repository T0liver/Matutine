package hu.toliver.matutine.edition

import kotlinx.serialization.Serializable

const val DEFAULT_AI_PROVIDER = "gemini"

@Serializable
data class ServerProcessConfig(
    val sharedToken: String = "",
    val timezone: String = "",
    val generationTime: String = "",
    val aiProvider: String = DEFAULT_AI_PROVIDER,
)

fun decodeServerProcessConfig(json: String): ServerProcessConfig =
    editionJson.decodeFromString(ServerProcessConfig.serializer(), json)

fun encodeServerProcessConfig(config: ServerProcessConfig): String =
    editionJson.encodeToString(ServerProcessConfig.serializer(), config)
