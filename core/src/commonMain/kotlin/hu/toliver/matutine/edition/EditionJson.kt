package hu.toliver.matutine.edition

import kotlinx.serialization.json.Json

val editionJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

fun decodeEdition(json: String): Edition = editionJson.decodeFromString(Edition.serializer(), json)

fun encodeEdition(edition: Edition): String = editionJson.encodeToString(Edition.serializer(), edition)
