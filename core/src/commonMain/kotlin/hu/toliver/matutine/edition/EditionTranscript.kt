package hu.toliver.matutine.edition

object EditionTranscript {
    const val BRIEFING = "Briefing"
    const val CALENDAR = "Calendar"
    const val TASKS = "Tasks"
    const val SAMPLE = "SAMPLE EDITION"
    const val SAVED = "Saved."
    const val SAVE_FAILED = "Couldn't save. Your text is still here."

    private val headers = setOf(BRIEFING, CALENDAR, TASKS)
    private val taskMark = Regex("""^\[(x|X| )] ?(.*)$""")

    fun render(edition: Edition): String = buildString {
        appendLine(edition.mastheadName)
        appendLine(edition.dateLine)
        if (edition.isSample) appendLine(SAMPLE)
        appendLine()
        appendLine(BRIEFING)
        if (edition.briefing.isNotEmpty()) appendLine(edition.briefing)
        appendLine()
        edition.stories.forEach { story ->
            appendLine(story.headline)
            appendLine(story.sourceName)
            if (story.deck.isNotEmpty()) appendLine(story.deck)
            if (story.sourceUrl.isNotEmpty()) appendLine(story.sourceUrl)
            appendLine()
        }
        appendLine(CALENDAR)
        edition.events.forEach { event ->
            appendLine(event.title)
            appendLine(event.timeLabel)
            appendLine()
        }
        if (edition.events.isEmpty()) appendLine()
        appendLine(TASKS)
        edition.tasks.forEach { task ->
            val mark = if (task.completed) "x" else " "
            appendLine("[$mark] ${task.title}")
        }
    }.trimEnd()

    fun apply(base: Edition, text: String): Edition {
        val parsed = parse(text)
        return base.copy(
            mastheadName = parsed.mastheadName,
            dateLine = parsed.dateLine,
            isSample = parsed.isSample,
            briefing = parsed.briefing,
            stories = parsed.stories.mapIndexed { index, story ->
                story.copy(rank = rankForSourceOrder(index))
            },
            events = parsed.events,
            tasks = parsed.tasks,
        )
    }

    private data class Parsed(
        val mastheadName: String,
        val dateLine: String,
        val isSample: Boolean,
        val briefing: String,
        val stories: List<EditionStory>,
        val events: List<EditionEvent>,
        val tasks: List<EditionTask>,
    )

    private enum class Section {
        Stories,
        Calendar,
        Tasks,
    }

    private fun parse(text: String): Parsed {
        val blocks = sectionBlocks(text)
        if (blocks.isEmpty()) {
            return Parsed("", "", false, "", emptyList(), emptyList(), emptyList())
        }
        var index = 0
        var mastheadName = ""
        var dateLine = ""
        var isSample = false
        if (blocks.first().first().trim() !in headers) {
            val lines = blocks.first().map { it.trim() }.filter { it.isNotEmpty() && it != SAMPLE }
            isSample = blocks.first().any { it.trim() == SAMPLE }
            mastheadName = lines.getOrElse(0) { "" }
            dateLine = lines.drop(1).joinToString("\n")
            index = 1
        }
        var briefing = ""
        val stories = mutableListOf<EditionStory>()
        val events = mutableListOf<EditionEvent>()
        val tasks = mutableListOf<EditionTask>()
        var section = Section.Stories
        while (index < blocks.size) {
            val lines = blocks[index].map { it.trim() }.filter { it.isNotEmpty() }
            index += 1
            if (lines.isEmpty()) continue
            when (lines.first()) {
                BRIEFING -> {
                    briefing = lines.drop(1).joinToString("\n")
                    section = Section.Stories
                }
                CALENDAR -> {
                    events += eventsFrom(lines.drop(1))
                    section = Section.Calendar
                }
                TASKS -> {
                    tasks += tasksFrom(lines.drop(1))
                    section = Section.Tasks
                }
                else -> when (section) {
                    Section.Stories -> stories += storyFrom(lines)
                    Section.Calendar -> events += eventsFrom(lines)
                    Section.Tasks -> tasks += tasksFrom(lines)
                }
            }
        }
        return Parsed(mastheadName, dateLine, isSample, briefing, stories, events, tasks)
    }

    private fun sectionBlocks(text: String): List<List<String>> {
        val blocks = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        fun flush() {
            if (current.isNotEmpty()) {
                blocks += current
                current = mutableListOf()
            }
        }
        text.replace("\r\n", "\n").replace('\r', '\n').split('\n').forEach { line ->
            if (line.isBlank()) {
                flush()
            } else {
                current += line
            }
        }
        flush()
        val split = mutableListOf<List<String>>()
        blocks.forEach { block ->
            var start = 0
            block.forEachIndexed { index, line ->
                if (index > start && line.trim() in headers) {
                    split += block.subList(start, index)
                    start = index
                }
            }
            if (start < block.size) split += block.subList(start, block.size)
        }
        return split
    }

    private fun storyFrom(lines: List<String>): EditionStory {
        val url = lines.last().takeIf(::isUrl).orEmpty()
        val body = if (url.isEmpty()) lines else lines.dropLast(1)
        return EditionStory(
            headline = body.getOrElse(0) { "" },
            sourceName = body.getOrElse(1) { "" },
            deck = body.drop(2).joinToString("\n"),
            sourceUrl = url,
            rank = StoryRank.note,
        )
    }

    private fun eventsFrom(lines: List<String>): List<EditionEvent> {
        val events = mutableListOf<EditionEvent>()
        var index = 0
        while (index < lines.size) {
            events += EditionEvent(
                title = lines[index],
                timeLabel = lines.getOrElse(index + 1) { "" },
            )
            index += 2
        }
        return events
    }

    private fun tasksFrom(lines: List<String>): List<EditionTask> =
        lines.mapNotNull { line ->
            val match = taskMark.matchEntire(line)
            val title = match?.groupValues?.get(2) ?: line
            if (title.isBlank()) {
                null
            } else {
                EditionTask(
                    title = title,
                    completed = match?.groupValues?.get(1).equals("x", ignoreCase = true),
                )
            }
        }

    private fun isUrl(line: String): Boolean =
        line.startsWith("https://") || line.startsWith("http://")
}
