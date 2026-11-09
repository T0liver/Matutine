package hu.toliver.matutine.edition

import kotlinx.serialization.Serializable

@Serializable
enum class StoryRank {
    lead,
    secondary,
    note,
}

@Serializable
enum class EditionRole {
    morning,
    extra,
}

@Serializable
data class EditionStory(
    val headline: String,
    val sourceName: String,
    val deck: String,
    val sourceUrl: String,
    val rank: StoryRank,
)

@Serializable
data class EditionEvent(
    val title: String,
    val timeLabel: String,
)

@Serializable
data class EditionTask(
    val title: String,
    val completed: Boolean = false,
)

@Serializable
data class Edition(
    val id: String,
    val revision: Long,
    val dayKey: String,
    val role: EditionRole,
    val mastheadName: String,
    val dateLine: String,
    val isSample: Boolean,
    val briefing: String,
    val stories: List<EditionStory>,
    val events: List<EditionEvent>,
    val tasks: List<EditionTask>,
    val sourceNotes: List<String> = emptyList(),
)
