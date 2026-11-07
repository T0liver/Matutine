package hu.toliver.matutine.edition

import kotlin.test.Test
import kotlin.test.assertEquals

class EditionCopyTest {

    @Test
    fun emptySectionCopyMatchesAcceptanceCriteria() {
        assertEquals("No stories yet.", EditionCopy.NO_STORIES)
        assertEquals("No events today.", EditionCopy.NO_EVENTS)
        assertEquals("No tasks yet.", EditionCopy.NO_TASKS)
    }
}
