package hu.toliver.matutine.edition

import kotlinx.serialization.Serializable

@Serializable
data class ReaderSettings(
    val topics: List<String> = emptyList(),
)

fun decodeReaderSettings(json: String): ReaderSettings =
    editionJson.decodeFromString(ReaderSettings.serializer(), json)

fun encodeReaderSettings(settings: ReaderSettings): String =
    editionJson.encodeToString(ReaderSettings.serializer(), settings)

/** Selected choices, then the free line, then day lines. Blank lines are not topics. */
fun topicsFromInterests(
    selectedChoices: List<String>,
    freeLine: String,
    dayLines: List<String>,
): List<String> =
    (selectedChoices + listOf(freeLine) + dayLines).map { it.trim() }.filter { it.isNotEmpty() }
