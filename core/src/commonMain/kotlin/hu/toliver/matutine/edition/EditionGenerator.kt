package hu.toliver.matutine.edition

import kotlinx.datetime.LocalDate

object EditionGenerator {
    fun fromFeedResults(
        date: LocalDate,
        parsed: List<Result<List<FeedItem>>>,
        existing: Edition?,
        topics: List<String> = emptyList(),
    ): Edition {
        if (parsed.none { it.isSuccess }) {
            error(FEED_DIDNT_LOAD)
        }
        val items = parsed.mapNotNull { it.getOrNull() }.flatten()
        val activeTopics = topics.map { it.trim() }.filter { it.isNotEmpty() }
        val stories = items.withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<FeedItem>> { topicFit(it.value, activeTopics) }
                    .thenBy { it.index },
            )
            .mapIndexed { index, indexed ->
                val item = indexed.value
                EditionStory(
                    headline = item.headline,
                    sourceName = item.sourceName,
                    deck = item.deck,
                    sourceUrl = item.sourceUrl,
                    rank = rankForSourceOrder(index),
                )
            }
        val personal = existing != null && !existing.isSample
        val failures = parsed.count { it.isFailure }
        return Edition(
            id = if (personal) existing.id else "morning-${date.toDayKey()}",
            revision = if (personal) existing.revision + 1 else 1,
            dayKey = date.toDayKey(),
            role = EditionRole.morning,
            mastheadName = "Matutine",
            dateLine = date.toDateLine(),
            isSample = false,
            briefing = extractiveBriefing(items),
            stories = stories,
            events = emptyList(),
            tasks = emptyList(),
            sourceNotes = List(failures) { FEED_DIDNT_LOAD },
        )
    }
}

internal fun topicFit(item: FeedItem, topics: List<String>): Int {
    val headline = item.headline.lowercase()
    val deck = item.deck.lowercase()
    return topics.count { topic ->
        val needle = topic.lowercase()
        headline.contains(needle) || deck.contains(needle)
    }
}
