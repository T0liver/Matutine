package hu.toliver.matutine.edition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditionTranscriptTest {
    private val edition = Edition(
        id = "morning-2026-11-10",
        revision = 4,
        dayKey = "2026-11-10",
        role = EditionRole.morning,
        mastheadName = "Matutine",
        dateLine = "Tuesday, November 10, 2026",
        isSample = true,
        briefing = "The harbor is open.\nShips are moving.",
        stories = listOf(
            EditionStory(
                headline = "Lead headline",
                sourceName = "City Desk",
                deck = "Deck one.",
                sourceUrl = "https://example.com/lead",
                rank = StoryRank.lead,
            ),
            EditionStory(
                headline = "Second headline",
                sourceName = "Wire",
                deck = "Deck two.",
                sourceUrl = "https://example.com/two",
                rank = StoryRank.secondary,
            ),
            EditionStory(
                headline = "Note headline",
                sourceName = "Notes",
                deck = "",
                sourceUrl = "",
                rank = StoryRank.note,
            ),
        ),
        events = listOf(
            EditionEvent("Standup", "09:00"),
            EditionEvent("Lunch", "12:30"),
        ),
        tasks = listOf(
            EditionTask("Milk"),
            EditionTask("File notes", completed = true),
        ),
        sourceNotes = listOf("Kept note"),
    )

    @Test
    fun renderRoundTripsTheEditionWithoutRankWords() {
        val text = EditionTranscript.render(edition)

        assertTrue(text.contains("Matutine"))
        assertTrue(text.contains("Tuesday, November 10, 2026"))
        assertTrue(text.contains(EditionTranscript.SAMPLE))
        assertTrue(text.contains("Briefing"))
        assertTrue(text.contains("The harbor is open."))
        assertTrue(text.contains("Lead headline"))
        assertTrue(text.contains("Calendar"))
        assertTrue(text.contains("Tasks"))
        assertTrue(text.contains("[ ] Milk"))
        assertTrue(text.contains("[x] File notes"))
        assertFalse(text.lineSequence().any { it == "lead" || it == "secondary" || it == "note" })

        val applied = EditionTranscript.apply(edition, text)

        assertEquals(edition.mastheadName, applied.mastheadName)
        assertEquals(edition.dateLine, applied.dateLine)
        assertEquals(edition.dayKey, applied.dayKey)
        assertEquals(edition.id, applied.id)
        assertEquals(edition.role, applied.role)
        assertEquals(edition.revision, applied.revision)
        assertEquals(edition.sourceNotes, applied.sourceNotes)
        assertTrue(applied.isSample)
        assertEquals(edition.briefing, applied.briefing)
        assertEquals(edition.stories, applied.stories)
        assertEquals(edition.events, applied.events)
        assertEquals(edition.tasks, applied.tasks)
        assertEquals(text, EditionTranscript.render(applied))
    }

    @Test
    fun printedDateChangesWithoutTheDayKey() {
        val text = EditionTranscript.render(edition).replace(
            "Tuesday, November 10, 2026",
            "Wednesday, November 11, 2026",
        )

        val applied = EditionTranscript.apply(edition, text)

        assertEquals("Wednesday, November 11, 2026", applied.dateLine)
        assertEquals("2026-11-10", applied.dayKey)
        assertEquals(edition.id, applied.id)
    }

    @Test
    fun reorderingStoryBlocksReappliesRanks() {
        val text = EditionTranscript.render(edition)
            .replace(
                """
                Lead headline
                City Desk
                Deck one.
                https://example.com/lead
                """.trimIndent(),
                "LEAD_BLOCK",
            )
            .replace(
                """
                Second headline
                Wire
                Deck two.
                https://example.com/two
                """.trimIndent(),
                """
                Lead headline
                City Desk
                Deck one.
                https://example.com/lead
                """.trimIndent(),
            )
            .replace(
                "LEAD_BLOCK",
                """
                Second headline
                Wire
                Deck two.
                https://example.com/two
                """.trimIndent(),
            )

        val applied = EditionTranscript.apply(edition, text)

        assertEquals(listOf("Second headline", "Lead headline", "Note headline"), applied.stories.map { it.headline })
        assertEquals(
            listOf(StoryRank.lead, StoryRank.secondary, StoryRank.note),
            applied.stories.map { it.rank },
        )
        assertEquals("https://example.com/two", applied.stories.first().sourceUrl)
    }

    @Test
    fun addedTaskAndStoryLinesAreKept() {
        val text = EditionTranscript.render(edition) + "\n[ ] Walk\n"

        val withStory = text.replace(
            "Briefing\nThe harbor is open.\nShips are moving.\n",
            """
            Briefing
            The harbor is open.
            Ships are moving.

            Extra headline
            Extra source
            Extra deck.
            https://example.com/extra

            """.trimIndent() + "\n",
        )
        val applied = EditionTranscript.apply(edition, withStory)

        assertEquals("Walk", applied.tasks.last().title)
        assertFalse(applied.tasks.last().completed)
        assertEquals("Extra headline", applied.stories.first().headline)
        assertEquals(StoryRank.lead, applied.stories.first().rank)
        assertEquals("https://example.com/extra", applied.stories.first().sourceUrl)
    }
}
