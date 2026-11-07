package hu.toliver.matutine.edition

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditionLayoutTest {

    @Test
    fun wideLayoutRequiresDesktopWidthAndNotForcedSingleColumn() {
        assertTrue(usesWideEditionLayout(960.dp, forceSingleColumn = false))
        assertTrue(usesWideEditionLayout(1200.dp, forceSingleColumn = false))
        assertFalse(usesWideEditionLayout(959.dp, forceSingleColumn = false))
        assertFalse(usesWideEditionLayout(1200.dp, forceSingleColumn = true))
    }

    @Test
    fun twoColumnSplitPreservesRankOrderDownLeftThenRight() {
        val stories = listOf(
            story("a"),
            story("b"),
            story("c"),
            story("d"),
            story("e"),
        )
        val (left, right) = storiesForTwoColumns(stories)
        assertEquals(listOf("a", "b", "c"), left.map { it.headline })
        assertEquals(listOf("d", "e"), right.map { it.headline })
    }

    @Test
    fun twoColumnSplitHandlesOddCount() {
        val stories = listOf(story("1"), story("2"), story("3"))
        val (left, right) = storiesForTwoColumns(stories)
        assertEquals(2, left.size)
        assertEquals(1, right.size)
    }

    private fun story(headline: String) = EditionStory(
        headline = headline,
        sourceName = "Source",
        deck = "deck",
        sourceUrl = "https://example.com",
        rank = StoryRank.note,
    )
}
