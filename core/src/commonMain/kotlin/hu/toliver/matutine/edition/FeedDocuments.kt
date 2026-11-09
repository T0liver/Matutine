package hu.toliver.matutine.edition

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant

const val FEED_DIDNT_LOAD = "That feed didn't load."
const val CALENDAR_LINK_DIDNT_OPEN = "That calendar link didn't open."
const val CALDAV_DIDNT_LOAD = "That CalDAV calendar didn't load."
const val MORNING_EDITION_NOT_READY = "Today's morning edition is not available yet."

data class FeedItem(
    val sourceName: String,
    val headline: String,
    val deck: String,
    val sourceUrl: String,
)

fun parseFeedDocument(document: String): List<FeedItem> {
    if (Regex("<rss\\b", RegexOption.IGNORE_CASE).containsMatchIn(document)) {
        return parseRss(document)
    }
    if (Regex("<feed\\b", RegexOption.IGNORE_CASE).containsMatchIn(document)) {
        return parseAtom(document)
    }
    error(FEED_DIDNT_LOAD)
}

internal fun parseRss(document: String): List<FeedItem> {
    val itemPattern = Regex("<item\\b[^>]*>([\\s\\S]*?)</item>", RegexOption.IGNORE_CASE)
    val items = itemPattern.findAll(document).toList()
    val head = if (items.isEmpty()) document else document.substring(0, items.first().range.first)
    val sourceName = tagText(head, "title").ifBlank { "Sample" }
    return items.map { match ->
        val body = match.groupValues[1]
        FeedItem(
            sourceName = sourceName,
            headline = tagText(body, "title"),
            deck = tagText(body, "description"),
            sourceUrl = tagText(body, "link"),
        )
    }.filter { it.headline.isNotBlank() }
}

internal fun parseAtom(document: String): List<FeedItem> {
    val entryPattern = Regex("<entry\\b[^>]*>([\\s\\S]*?)</entry>", RegexOption.IGNORE_CASE)
    val entries = entryPattern.findAll(document).toList()
    val head = if (entries.isEmpty()) document else document.substring(0, entries.first().range.first)
    val sourceName = tagText(head, "title").ifBlank { "Feed" }
    return entries.map { match ->
        val body = match.groupValues[1]
        FeedItem(
            sourceName = sourceName,
            headline = tagText(body, "title"),
            deck = tagText(body, "summary").ifBlank { tagText(body, "content") },
            sourceUrl = atomLink(body),
        )
    }.filter { it.headline.isNotBlank() }
}

internal fun parseIcs(calendar: String): List<EditionEvent> {
    val eventPattern = Regex("BEGIN:VEVENT([\\s\\S]*?)END:VEVENT", RegexOption.IGNORE_CASE)
    return eventPattern.findAll(calendar).mapNotNull { match ->
        val body = match.groupValues[1]
        val title = lineValue(body, "SUMMARY")?.trim().orEmpty()
        if (title.isEmpty()) return@mapNotNull null
        EditionEvent(title = title, timeLabel = formatIcsTime(lineValue(body, "DTSTART").orEmpty()))
    }.toList()
}

internal fun parseIcsOccurrences(calendar: String, zone: TimeZone): List<CalendarOccurrence> {
    val unfolded = unfoldIcs(calendar)
    val eventPattern = Regex("BEGIN:VEVENT([\\s\\S]*?)END:VEVENT", RegexOption.IGNORE_CASE)
    return eventPattern.findAll(unfolded).mapNotNull { match ->
        try {
            val properties = icsProperties(match.groupValues[1])
            val title = properties.firstOrNull { it.name == "SUMMARY" }?.value?.let(::unescapeIcs)?.trim().orEmpty()
            if (title.isEmpty()) return@mapNotNull null
            val startProperty = properties.firstOrNull { it.name == "DTSTART" } ?: return@mapNotNull null
            val start = parseIcsDateTime(startProperty, zone) ?: return@mapNotNull null
            val endProperty = properties.firstOrNull { it.name == "DTEND" }
            val end = endProperty?.let { parseIcsDateTime(it, zone) } ?: start
            CalendarOccurrence(
                title = title,
                startEpochSecond = start.epochSecond,
                endEpochSecond = end.epochSecond,
                allDay = start.allDay,
            )
        } catch (_: IllegalArgumentException) {
            null
        }
    }.toList()
}

internal fun calendarUrlSendsPassword(url: String): Boolean {
    val rest = url.trim().substringAfter("://", missingDelimiterValue = "")
    if (rest.isEmpty()) return false
    val authority = rest.substringBefore('/').substringBefore('?').substringBefore('#')
    return authority.substringBeforeLast('@').let { userInfo ->
        authority.contains('@') && userInfo.isNotEmpty()
    }
}

private data class IcsProperty(val name: String, val params: String, val value: String)

private data class IcsDateTime(val epochSecond: Long, val allDay: Boolean)

private fun unfoldIcs(calendar: String): String =
    calendar.replace("\r\n", "\n").replace('\r', '\n').replace(Regex("\n[ \t]"), "")

private fun icsProperties(body: String): List<IcsProperty> =
    body.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { line ->
        val colon = line.indexOf(':')
        if (colon <= 0) return@mapNotNull null
        val left = line.substring(0, colon)
        IcsProperty(
            name = left.substringBefore(';').uppercase(),
            params = left.substringAfter(';', ""),
            value = line.substring(colon + 1).trim(),
        )
    }.toList()

private fun parseIcsDateTime(property: IcsProperty, zone: TimeZone): IcsDateTime? {
    val raw = property.value.trim()
    val valueType = icsParameter(property.params, "VALUE")
    val dateOnly = valueType.equals("DATE", ignoreCase = true) ||
        (valueType == null && !raw.contains('T'))
    if (dateOnly) {
        if (raw.length < 8 || !raw.take(8).all { it.isDigit() }) return null
        val date = LocalDate(
            raw.substring(0, 4).toInt(),
            raw.substring(4, 6).toInt(),
            raw.substring(6, 8).toInt(),
        )
        return IcsDateTime(date.atStartOfDayIn(TimeZone.UTC).epochSeconds, allDay = true)
    }
    val utc = raw.endsWith("Z", ignoreCase = true)
    val digits = raw.removeSuffix("Z").removeSuffix("z")
    val datePart = digits.substringBefore('T')
    val timePart = digits.substringAfter('T', "")
    if (datePart.length < 8 || timePart.length < 4) return null
    if (!datePart.take(8).all { it.isDigit() } || !timePart.take(4).all { it.isDigit() }) return null
    val hour = timePart.substring(0, 2).toInt()
    val minute = timePart.substring(2, 4).toInt()
    val second = if (timePart.length >= 6 && timePart.substring(4, 6).all { it.isDigit() }) {
        timePart.substring(4, 6).toInt()
    } else {
        0
    }
    if (hour !in 0..23 || minute !in 0..59 || second !in 0..59) return null
    val local = LocalDateTime(
        datePart.substring(0, 4).toInt(),
        datePart.substring(4, 6).toInt(),
        datePart.substring(6, 8).toInt(),
        hour,
        minute,
        second,
    )
    val eventZone = if (utc) {
        TimeZone.UTC
    } else {
        icsTimeZone(property.params) ?: zone
    }
    return IcsDateTime(local.toInstant(eventZone).epochSeconds, allDay = false)
}

private fun icsParameter(params: String, name: String): String? =
    params.split(';').firstNotNullOfOrNull { param ->
        val key = param.substringBefore('=').trim()
        if (!key.equals(name, ignoreCase = true)) return@firstNotNullOfOrNull null
        param.substringAfter('=', "").trim().trim('"')
    }

private fun icsTimeZone(params: String): TimeZone? {
    val tzid = Regex("""TZID="?([^";]+)"?""", RegexOption.IGNORE_CASE).find(params)?.groupValues?.get(1)?.trim()
    if (tzid.isNullOrEmpty()) return null
    return runCatching { TimeZone.of(tzid) }.getOrNull()
}

private fun unescapeIcs(value: String): String {
    val out = StringBuilder()
    var index = 0
    while (index < value.length) {
        val current = value[index]
        if (current == '\\' && index + 1 < value.length) {
            when (val next = value[index + 1]) {
                'n', 'N' -> out.append('\n')
                else -> out.append(next)
            }
            index += 2
        } else {
            out.append(current)
            index += 1
        }
    }
    return out.toString()
}

internal fun formatIcsTime(raw: String): String {
    val digits = raw.substringAfter('T', raw).takeWhile { it.isDigit() }
    if (digits.length < 4) return ""
    val hour = digits.substring(0, 2).toIntOrNull() ?: return ""
    val minute = digits.substring(2, 4)
    return "$hour:$minute"
}

private fun tagText(xml: String, tag: String): String {
    val match = Regex("<$tag\\b[^>]*>([\\s\\S]*?)</$tag>", RegexOption.IGNORE_CASE).find(xml) ?: return ""
    return unescapeXml(unwrapCdata(match.groupValues[1].trim()))
}

private fun atomLink(xml: String): String {
    val tags = Regex("<(?:[\\w.-]+:)?link\\b([^>]*)/?>", RegexOption.IGNORE_CASE)
        .findAll(xml)
        .map { it.groupValues[1] }
        .toList()
    val alternate = tags.firstOrNull { attributes ->
        Regex("rel\\s*=\\s*['\"]alternate['\"]", RegexOption.IGNORE_CASE).containsMatchIn(attributes)
    }
    val chosen = alternate ?: tags.firstOrNull()
    if (chosen != null) {
        val href = Regex("href\\s*=\\s*['\"]([^'\"]+)['\"]", RegexOption.IGNORE_CASE).find(chosen)
        if (href != null) return unescapeXml(href.groupValues[1].trim())
    }
    return tagText(xml, "link")
}

private fun unwrapCdata(text: String): String {
    val match = Regex("^<!\\[CDATA\\[([\\s\\S]*?)\\]\\]>$").find(text) ?: return text
    return match.groupValues[1].trim()
}

private fun lineValue(block: String, name: String): String? =
    block.lineSequence()
        .map { it.trim() }
        .firstOrNull { line ->
            line.startsWith("$name:", ignoreCase = true) || line.startsWith("$name;", ignoreCase = true)
        }
        ?.substringAfter(':')

private fun unescapeXml(text: String): String =
    text.replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
