package hu.toliver.matutine.edition

internal object EditionCopy {
    const val NO_STORIES = "No stories yet."
    const val NO_EVENTS = "No events today."
    const val NO_TASKS = "No tasks yet."
    const val SETTING_TODAYS_EDITION = "Setting today's edition."
    const val WHAT_DO_YOU_WANT_TO_READ_ABOUT = "What do you want to read about?"
    const val WHAT_SHOULD_THE_PAGE_NOTICE = "What should the page notice about your day?"
}

internal fun showsNoTopicLine(isSample: Boolean, topics: List<String>): Boolean =
    !isSample && topics.none { it.isNotBlank() }
