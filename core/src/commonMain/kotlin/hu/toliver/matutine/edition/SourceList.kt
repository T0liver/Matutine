package hu.toliver.matutine.edition

import kotlinx.serialization.Serializable

@Serializable
data class SourceList(
    val id: String = "",
    val revision: Long = 0,
    val feeds: List<String> = emptyList(),
    val calendars: List<String> = emptyList(),
)

fun decodeSourceList(json: String): SourceList =
    editionJson.decodeFromString(SourceList.serializer(), json)

fun encodeSourceList(sourceList: SourceList): String =
    editionJson.encodeToString(SourceList.serializer(), sourceList)
