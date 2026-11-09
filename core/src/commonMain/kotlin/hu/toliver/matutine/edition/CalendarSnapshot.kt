package hu.toliver.matutine.edition

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class CalendarOccurrence(
    val title: String,
    val startEpochSecond: Long,
    val endEpochSecond: Long,
    val allDay: Boolean = false,
)

@Serializable
data class CalendarSnapshot(
    val occurrences: List<CalendarOccurrence> = emptyList(),
)

sealed interface DeviceCalendarRead {
    data class Events(val occurrences: List<CalendarOccurrence>) : DeviceCalendarRead
    data object Empty : DeviceCalendarRead
    data object Failed : DeviceCalendarRead
    data object Unavailable : DeviceCalendarRead
}

fun decodeCalendarSnapshot(json: String): CalendarSnapshot =
    editionJson.decodeFromString(CalendarSnapshot.serializer(), json)

fun encodeCalendarSnapshot(snapshot: CalendarSnapshot): String =
    editionJson.encodeToString(CalendarSnapshot.serializer(), snapshot)

fun CalendarOccurrence.startsOn(day: LocalDate, zone: TimeZone): Boolean {
    val startDate = if (allDay) {
        Instant.fromEpochSeconds(startEpochSecond).toLocalDateTime(TimeZone.UTC).date
    } else {
        Instant.fromEpochSeconds(startEpochSecond).toLocalDateTime(zone).date
    }
    return startDate == day
}

/** Identity is start, end, and trimmed title at second precision. Source is not part of it. */
fun editionEventsFrom(
    sources: List<List<CalendarOccurrence>>,
    day: LocalDate,
    zone: TimeZone,
): List<EditionEvent> {
    val seen = mutableSetOf<Triple<Long, Long, String>>()
    return sources.flatten()
        .filter { it.startsOn(day, zone) }
        .sortedWith(compareBy({ it.startEpochSecond }, { it.title.trim() }))
        .mapNotNull { occurrence ->
            val title = occurrence.title.trim()
            if (title.isEmpty()) return@mapNotNull null
            val identity = Triple(occurrence.startEpochSecond, occurrence.endEpochSecond, title)
            if (!seen.add(identity)) return@mapNotNull null
            EditionEvent(title = title, timeLabel = visibleStart(occurrence, zone))
        }
}

private fun visibleStart(occurrence: CalendarOccurrence, zone: TimeZone): String {
    if (occurrence.allDay) return "0:00"
    val local = Instant.fromEpochSeconds(occurrence.startEpochSecond).toLocalDateTime(zone)
    return "${local.hour}:${local.minute.toString().padStart(2, '0')}"
}
