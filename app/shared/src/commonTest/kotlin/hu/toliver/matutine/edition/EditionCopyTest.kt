package hu.toliver.matutine.edition

import hu.toliver.matutine.settings.ADD_ANOTHER
import hu.toliver.matutine.settings.InterestChoices
import hu.toliver.matutine.settings.SAVE_TOPICS
import hu.toliver.matutine.settings.draftFromTopics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditionCopyTest {

    @Test
    fun emptySectionCopyMatchesAcceptanceCriteria() {
        assertEquals("No stories yet.", EditionCopy.NO_STORIES)
        assertEquals("No events today.", EditionCopy.NO_EVENTS)
        assertEquals("No tasks yet.", EditionCopy.NO_TASKS)
        assertEquals("Setting today's edition.", EditionCopy.SETTING_TODAYS_EDITION)
        assertEquals("What do you want to read about?", EditionCopy.WHAT_DO_YOU_WANT_TO_READ_ABOUT)
        assertEquals("What should the page notice about your day?", EditionCopy.WHAT_SHOULD_THE_PAGE_NOTICE)
        assertEquals("Add another", ADD_ANOTHER)
        assertEquals("Save", SAVE_TOPICS)
        assertEquals(
            listOf(
                "Video games",
                "Technology",
                "Biology",
                "Finance",
                "Science",
                "Sports",
                "World news",
                "Culture",
                "Health",
                "Local",
            ),
            InterestChoices,
        )
    }

    @Test
    fun draftFromTopicsKeepsChoicesThenTheFreeLineThenTheDayLine() {
        val draft = draftFromTopics(listOf("Technology", "Video games", "Rust", "school"))

        assertEquals(listOf("Technology", "Video games"), draft.selected)
        assertEquals("Rust", draft.freeLine)
        assertEquals(listOf("school"), draft.dayLines)
    }

    @Test
    fun noTopicLineOpensInterestsOnlyOnThePersonsEdition() {
        assertTrue(showsNoTopicLine(isSample = false, topics = emptyList()))
        assertTrue(showsNoTopicLine(isSample = false, topics = listOf("  ")))
        assertFalse(showsNoTopicLine(isSample = true, topics = emptyList()))
        assertFalse(showsNoTopicLine(isSample = true, topics = listOf("Technology")))
        assertFalse(showsNoTopicLine(isSample = false, topics = listOf("Technology", "Video games")))
    }
}
