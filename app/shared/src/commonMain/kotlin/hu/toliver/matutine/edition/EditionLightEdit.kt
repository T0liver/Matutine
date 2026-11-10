package hu.toliver.matutine.edition

sealed interface LightEditField {
    data object Masthead : LightEditField

    data object DateLine : LightEditField

    data object Briefing : LightEditField

    data class StoryHeadline(val index: Int) : LightEditField

    data class StorySource(val index: Int) : LightEditField

    data class StoryDeck(val index: Int) : LightEditField

    data class EventTitle(val index: Int) : LightEditField

    data class EventTime(val index: Int) : LightEditField
}

fun applyLightEdit(edition: Edition, field: LightEditField, value: String): Edition =
    when (field) {
        LightEditField.Masthead -> edition.copy(mastheadName = value)
        LightEditField.DateLine -> edition.copy(dateLine = value)
        LightEditField.Briefing -> edition.copy(briefing = value)
        is LightEditField.StoryHeadline -> edition.withStory(field.index) { it.copy(headline = value) }
        is LightEditField.StorySource -> edition.withStory(field.index) { it.copy(sourceName = value) }
        is LightEditField.StoryDeck -> edition.withStory(field.index) { it.copy(deck = value) }
        is LightEditField.EventTitle -> edition.withEvent(field.index) { it.copy(title = value) }
        is LightEditField.EventTime -> edition.withEvent(field.index) { it.copy(timeLabel = value) }
    }

private fun Edition.withStory(index: Int, update: (EditionStory) -> EditionStory): Edition {
    if (index !in stories.indices) return this
    val next = stories.toMutableList()
    next[index] = update(stories[index])
    return copy(stories = next)
}

private fun Edition.withEvent(index: Int, update: (EditionEvent) -> EditionEvent): Edition {
    if (index !in events.indices) return this
    val next = events.toMutableList()
    next[index] = update(events[index])
    return copy(events = next)
}
