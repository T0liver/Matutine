package hu.toliver.matutine.edition

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

val editionJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
}

fun decodeEdition(json: String): Edition = editionJson.decodeFromString(Edition.serializer(), json)

fun encodeEdition(edition: Edition): String = editionJson.encodeToString(Edition.serializer(), edition)

fun encodeEditionList(editions: List<Edition>): String =
    editionJson.encodeToString(ListSerializer(Edition.serializer()), editions)

fun decodeEditionList(json: String): List<Edition> =
    editionJson.decodeFromString(ListSerializer(Edition.serializer()), json)
