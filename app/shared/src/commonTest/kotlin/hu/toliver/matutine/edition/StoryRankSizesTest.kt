package hu.toliver.matutine.edition

import kotlin.test.Test
import kotlin.test.assertTrue

class StoryRankSizesTest {

    @Test
    fun compactHeadlineSizesFollowLeadSecondaryNoteOrder() {
        val lead = StoryRank.lead.compactHeadlineSp()
        val secondary = StoryRank.secondary.compactHeadlineSp()
        val note = StoryRank.note.compactHeadlineSp()
        assertTrue(lead > secondary)
        assertTrue(secondary > note)
    }
}
