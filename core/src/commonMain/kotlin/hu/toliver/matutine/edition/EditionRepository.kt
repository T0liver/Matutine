package hu.toliver.matutine.edition

import kotlinx.datetime.LocalDate

interface EditionStore {
    /** Null when the source-list document is absent. An empty document is present. */
    fun readSourceList(): SourceList?

    fun writeSourceList(sourceList: SourceList)

    /** Null when the settings document is absent. An empty topic list is present. */
    fun readSettings(): ReaderSettings?

    fun writeSettings(settings: ReaderSettings)

    fun loadMorningEdition(dayKey: String): Edition?
    fun saveMorningEdition(edition: Edition)
}

class LocalEditionRepository(
    private val store: EditionStore,
    private val today: () -> LocalDate = { todayLocalDate() },
    private val forceSampleLoadFailure: () -> Boolean = { false },
    private val generateSample: (LocalDate) -> Edition = SampleGenerator::forDay,
    private val readFeed: suspend (String) -> String = { error(FEED_DIDNT_LOAD) },
) : EditionRepository {
    override suspend fun loadCurrentMorningEdition(): Result<Edition> = runCatching {
        val dayKey = today().toDayKey()
        store.loadMorningEdition(dayKey)?.let { return@runCatching it }

        if (store.readSourceList() != null) {
            error("Today's morning edition is not available yet.")
        }

        if (forceSampleLoadFailure()) {
            error("sample_load_failed")
        }

        generateSample(today()).also { sample ->
            store.saveMorningEdition(sample)
        }
    }

    override suspend fun retrySample(): Result<Edition> = runCatching {
        if (store.readSourceList() != null) {
            error("Sample is not used when a source list exists.")
        }
        if (forceSampleLoadFailure()) {
            error("sample_load_failed")
        }
        generateSample(today()).also { store.saveMorningEdition(it) }
    }

    override fun savedFeedUrls(): List<String> = store.readSourceList()?.feeds.orEmpty()

    override fun savedTopics(): List<String> = store.readSettings()?.topics.orEmpty()

    override fun saveTopics(topics: List<String>) {
        store.writeSettings(
            ReaderSettings(
                topics = topics.map { it.trim() }.filter { it.isNotEmpty() },
            ),
        )
    }

    override suspend fun saveFeedUrl(url: String): Result<Unit> = runCatching {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) error(FEED_DIDNT_LOAD)
        val document = readDocument(trimmed)
        try {
            parseFeedDocument(document)
        } catch (error: Throwable) {
            rethrowCancellation(error)
            error(FEED_DIDNT_LOAD)
        }
        val current = store.readSourceList() ?: SourceList()
        if (current.feeds == listOf(trimmed)) return@runCatching
        store.writeSourceList(
            current.copy(
                id = current.id.ifBlank { "sources" },
                revision = current.revision + 1,
                feeds = listOf(trimmed),
            ),
        )
    }

    override suspend fun generateToday(): Result<Edition> = runCatching {
        val list = store.readSourceList()
        if (list == null || list.feeds.isEmpty()) {
            error(FEED_DIDNT_LOAD)
        }
        val date = today()
        val parsed = list.feeds.map { url ->
            runCatching { parseFeedDocument(readDocument(url)) }
        }
        val edition = EditionGenerator.fromFeedResults(
            date = date,
            parsed = parsed,
            existing = store.loadMorningEdition(date.toDayKey()),
            topics = store.readSettings()?.topics.orEmpty(),
        )
        store.saveMorningEdition(edition)
        edition
    }

    private suspend fun readDocument(url: String): String =
        try {
            readFeed(url)
        } catch (error: Throwable) {
            rethrowCancellation(error)
            error(FEED_DIDNT_LOAD)
        }
}

private fun rethrowCancellation(error: Throwable) {
    if (error is kotlin.coroutines.cancellation.CancellationException) throw error
}

interface EditionRepository {
    suspend fun loadCurrentMorningEdition(): Result<Edition>
    suspend fun retrySample(): Result<Edition>
    fun savedFeedUrls(): List<String>
    suspend fun saveFeedUrl(url: String): Result<Unit>
    fun savedTopics(): List<String>
    fun saveTopics(topics: List<String>)
    suspend fun generateToday(): Result<Edition>
}
