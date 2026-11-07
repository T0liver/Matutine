package hu.toliver.matutine.edition

import kotlinx.datetime.LocalDate

object SampleEdition {
    fun forDay(date: LocalDate): Edition {
        val dayKey = date.toDayKey()
        return Edition(
            id = "sample-$dayKey",
            revision = 1,
            dayKey = dayKey,
            role = EditionRole.morning,
            mastheadName = "Matutine",
            dateLine = date.toDateLine(),
            isSample = true,
            briefing =
                "Overnight wires bring a mixed picture: labor markets hold steady while " +
                    "policy makers signal patience on rates. Markets open attentively on " +
                    "earnings and a light data calendar.",
            stories = listOf(
                EditionStory(
                    headline = "Central banks signal a longer pause on rates",
                    sourceName = "Sample Wire",
                    deck = "Officials emphasize data dependence as inflation cools unevenly across regions.",
                    sourceUrl = "https://example.com/sample/lead",
                    rank = StoryRank.lead,
                ),
                EditionStory(
                    headline = "Tech firms outline cautious hiring plans",
                    sourceName = "Sample Herald",
                    deck = "Quarterly updates point to selective growth rather than broad expansion.",
                    sourceUrl = "https://example.com/sample/secondary",
                    rank = StoryRank.secondary,
                ),
                EditionStory(
                    headline = "Brief: Transit upgrades set for weekend",
                    sourceName = "City Desk",
                    deck = "A single track closure may add fifteen minutes to cross-town trips.",
                    sourceUrl = "https://example.com/sample/note",
                    rank = StoryRank.note,
                ),
            ),
            events = listOf(
                EditionEvent(title = "Team standup", timeLabel = "9:00"),
                EditionEvent(title = "Lunch with Alex", timeLabel = "12:30"),
            ),
            tasks = listOf(
                EditionTask(title = "Skim the overnight wires"),
                EditionTask(title = "Confirm dentist appointment"),
            ),
        )
    }
}
