package hu.toliver.matutine.edition

import kotlinx.datetime.LocalDate

object SampleGenerator {
    fun forDay(date: LocalDate): Edition {
        val items = BundledSources.feedItems()
        if (items.isEmpty()) error("sample_load_failed")
        val stories = items.mapIndexed { index, item ->
            EditionStory(
                headline = item.headline,
                sourceName = item.sourceName,
                deck = item.deck,
                sourceUrl = item.sourceUrl,
                rank = rankForSourceOrder(index),
            )
        }
        return Edition(
            id = "sample-${date.toDayKey()}",
            revision = 1,
            dayKey = date.toDayKey(),
            role = EditionRole.morning,
            mastheadName = "Matutine",
            dateLine = date.toDateLine(),
            isSample = true,
            briefing = extractiveBriefing(items),
            stories = stories,
            events = BundledSources.events(),
            tasks = emptyList(),
        )
    }
}

internal fun rankForSourceOrder(index: Int): StoryRank = when (index) {
    0 -> StoryRank.lead
    1 -> StoryRank.secondary
    else -> StoryRank.note
}

internal fun extractiveBriefing(items: List<FeedItem>): String {
    val summary = items.joinToString(" ") { item ->
        val text = item.deck.ifBlank { item.headline }.trim()
        if (text.endsWith(".")) text else "$text."
    }
    return "$summary Summary assembled from headlines."
}
