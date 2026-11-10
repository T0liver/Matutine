package hu.toliver.matutine.edition

import kotlinx.serialization.Serializable

const val DEFAULT_NOTIFICATION_TIME = "07:00"
const val EDITION_READY_NOTIFICATION_BODY = "Today's edition is ready."
const val EDITION_NOTIFICATION_TITLE = "Matutine"
const val DEFAULT_EDITION_THEME = "newsprint"
const val DEFAULT_EDITION_LAYOUT = "newsprint"

val EDITION_THEME_OPTIONS: List<Pair<String, String>> = listOf(
    DEFAULT_EDITION_THEME to "Newsprint",
)

val EDITION_LAYOUT_OPTIONS: List<Pair<String, String>> = listOf(
    DEFAULT_EDITION_LAYOUT to "Newsprint columns",
)

private val knownEditionThemes = EDITION_THEME_OPTIONS.map { it.first }.toSet()
private val knownEditionLayouts = EDITION_LAYOUT_OPTIONS.map { it.first }.toSet()

@Serializable
data class ReaderSettings(
    val topics: List<String> = emptyList(),
    val notificationTime: String = DEFAULT_NOTIFICATION_TIME,
    val notificationBody: String = EDITION_READY_NOTIFICATION_BODY,
    val editionTheme: String = DEFAULT_EDITION_THEME,
    val editionLayout: String = DEFAULT_EDITION_LAYOUT,
    val geminiKey: String = "",
)

fun ReaderSettings.resolvedNotificationTime(): String =
    notificationTime.takeIf { parseNotificationTime(it) != null } ?: DEFAULT_NOTIFICATION_TIME

fun ReaderSettings.resolvedNotificationBody(): String =
    notificationBody.trim().takeIf { it.isNotEmpty() } ?: EDITION_READY_NOTIFICATION_BODY

fun ReaderSettings.resolvedEditionTheme(): String {
    val normalized = editionTheme.trim().lowercase()
    return normalized.takeIf { it in knownEditionThemes } ?: DEFAULT_EDITION_THEME
}

fun ReaderSettings.resolvedEditionLayout(): String {
    val normalized = editionLayout.trim().lowercase()
    return normalized.takeIf { it in knownEditionLayouts } ?: DEFAULT_EDITION_LAYOUT
}

fun normalizeEditionThemeId(value: String): String? {
    val normalized = value.trim().lowercase()
    return normalized.takeIf { it in knownEditionThemes }
}

fun normalizeEditionLayoutId(value: String): String? {
    val normalized = value.trim().lowercase()
    return normalized.takeIf { it in knownEditionLayouts }
}

fun notificationTimeLabel(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

fun parseNotificationTime(value: String): Pair<Int, Int>? {
    val parts = value.trim().split(':')
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour to minute
}

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
