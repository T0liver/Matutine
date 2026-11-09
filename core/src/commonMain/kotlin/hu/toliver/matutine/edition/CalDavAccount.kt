package hu.toliver.matutine.edition

import kotlin.io.encoding.Base64
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable

@Serializable
data class CalDavAccount(
    val url: String,
    val username: String,
    val password: String,
)

fun decodeCalDavAccount(json: String): CalDavAccount =
    editionJson.decodeFromString(CalDavAccount.serializer(), json)

fun encodeCalDavAccount(account: CalDavAccount): String =
    editionJson.encodeToString(CalDavAccount.serializer(), account)

fun calDavReportDocument(): String = """
<?xml version="1.0" encoding="utf-8" ?>
<C:calendar-query xmlns:D="DAV:" xmlns:C="urn:ietf:params:xml:ns:caldav">
  <D:prop>
    <C:calendar-data/>
  </D:prop>
  <C:filter>
    <C:comp-filter name="VCALENDAR">
      <C:comp-filter name="VEVENT"/>
    </C:comp-filter>
  </C:filter>
</C:calendar-query>
""".trim()

fun basicAuthorizationValue(username: String, password: String): String {
    val token = Base64.encode("$username:$password".encodeToByteArray())
    return "Basic $token"
}

fun occurrencesFromCalDavResponse(body: String, zone: TimeZone): List<CalendarOccurrence> {
    val trimmed = body.trim()
    val looksLikeCalendar = trimmed.contains("BEGIN:VCALENDAR", ignoreCase = true) ||
        trimmed.contains("calendar-data", ignoreCase = true)
    if (!looksLikeCalendar) {
        if (trimmed.contains("multistatus", ignoreCase = true)) {
            if (calDavStatusFailed(trimmed)) error(CALDAV_DIDNT_LOAD)
            return emptyList()
        }
        error(CALDAV_DIDNT_LOAD)
    }
    val calendar = calendarBodies(trimmed)
    if (!calendar.contains("BEGIN:VCALENDAR", ignoreCase = true)) {
        if (calDavStatusFailed(trimmed)) error(CALDAV_DIDNT_LOAD)
        return emptyList()
    }
    return parseIcsOccurrences(calendar, zone)
}

private fun calDavStatusFailed(body: String): Boolean {
    val statuses = Regex("""HTTP/1\.[01]\s+(\d{3})""").findAll(body).map { it.groupValues[1].toInt() }.toList()
    if (statuses.isEmpty()) return false
    return statuses.none { it in 200..299 }
}

private fun calendarBodies(response: String): String {
    if (!response.contains("calendar-data", ignoreCase = true)) return response
    val pattern = Regex(
        """<(?:[\w.-]+:)?calendar-data\b[^>]*>([\s\S]*?)</(?:[\w.-]+:)?calendar-data>""",
        RegexOption.IGNORE_CASE,
    )
    return pattern.findAll(response).joinToString("\n") { match ->
        unwrapCdata(unescapeXml(match.groupValues[1].trim()))
    }
}

private fun unwrapCdata(text: String): String {
    val match = Regex("^<!\\[CDATA\\[([\\s\\S]*?)\\]\\]>$").find(text) ?: return text
    return match.groupValues[1]
}

private fun unescapeXml(text: String): String {
    val amp = text.replace("&amp;", "&")
    val numeric = Regex("&#(x[0-9A-Fa-f]+|[0-9]+);").replace(amp) { match ->
        val token = match.groupValues[1]
        val code = if (token[0] == 'x' || token[0] == 'X') token.drop(1).toInt(16) else token.toInt()
        if (code !in 0..0xFFFF) match.value else code.toChar().toString()
    }
    return numeric
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
}
