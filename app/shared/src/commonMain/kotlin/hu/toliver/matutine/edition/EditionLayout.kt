package hu.toliver.matutine.edition

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Breakpoint from UX-DR17 / Story 1.3 — wide newspaper layout at or above this width. */
val EditionWideLayoutMinWidth: Dp = 960.dp

/** Right rail width for calendar and tasks on wide desktop layout. */
val EditionRailWidth: Dp = 280.dp

fun usesWideEditionLayout(contentWidth: Dp, forceSingleColumn: Boolean): Boolean =
    !forceSingleColumn && contentWidth >= EditionWideLayoutMinWidth

/** Split ranked stories for a two-column broadsheet: read down the left column, then the right. */
fun storiesForTwoColumns(stories: List<EditionStory>): Pair<List<EditionStory>, List<EditionStory>> {
    if (stories.isEmpty()) return emptyList<EditionStory>() to emptyList()
    val midpoint = (stories.size + 1) / 2
    return stories.take(midpoint) to stories.drop(midpoint)
}
