package hu.toliver.matutine.edition

import kotlin.test.Test
import kotlin.test.assertEquals

class EditionLightEditTest {
    private val edition = Edition(
        id = "morning-2026-11-10",
        revision = 2,
        dayKey = "2026-11-10",
        role = EditionRole.morning,
        mastheadName = "Matutine",
        dateLine = "Tuesday, November 10, 2026",
        isSample = false,
        briefing = "Brief.",
        stories = listOf(
            EditionStory("Lead", "Desk", "Deck.", "https://example.com", StoryRank.lead),
        ),
        events = listOf(EditionEvent("Standup", "09:00")),
        tasks = emptyList(),
    )

    @Test
    fun dateLineChangeKeepsDayKey() {
        val next = applyLightEdit(
            edition,
            LightEditField.DateLine,
            "Wednesday, November 11, 2026",
        )
        assertEquals("Wednesday, November 11, 2026", next.dateLine)
        assertEquals(edition.dayKey, next.dayKey)
        assertEquals(edition.id, next.id)
    }

    @Test
    fun calendarTitleUpdatesEvent() {
        val next = applyLightEdit(edition, LightEditField.EventTitle(0), "Review")
        assertEquals("Review", next.events.first().title)
        assertEquals("09:00", next.events.first().timeLabel)
    }
}
