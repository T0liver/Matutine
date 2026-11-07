package hu.toliver.matutine.edition

/** Lead point sizes from DESIGN.md (compact single-column edition). */
internal object StoryRankSizes {
    const val LEAD_SP = 28
    const val SECONDARY_SP = 22
    const val NOTE_SP = 18
}

internal fun StoryRank.compactHeadlineSp(): Int = when (this) {
    StoryRank.lead -> StoryRankSizes.LEAD_SP
    StoryRank.secondary -> StoryRankSizes.SECONDARY_SP
    StoryRank.note -> StoryRankSizes.NOTE_SP
}
