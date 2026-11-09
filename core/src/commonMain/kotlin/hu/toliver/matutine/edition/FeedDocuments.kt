package hu.toliver.matutine.edition

const val FEED_DIDNT_LOAD = "That feed didn't load."

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
