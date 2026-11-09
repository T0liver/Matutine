package hu.toliver.matutine.edition

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

interface EditionStore {
    /** Null when the source-list document is absent. An empty document is present. */
    fun readSourceList(): SourceList?

    fun writeSourceList(sourceList: SourceList)

    /** Null when the settings document is absent. An empty topic list is present. */
    fun readSettings(): ReaderSettings?

    fun writeSettings(settings: ReaderSettings)

    fun loadMorningEdition(dayKey: String): Edition?
    fun saveMorningEdition(edition: Edition)

    /** Null when the snapshot document is absent. */
    fun readCalendarSnapshot(): CalendarSnapshot?

    fun writeCalendarSnapshot(snapshot: CalendarSnapshot)

    /** Null when the CalDAV account document is absent. */
    fun readCalDavAccount(): CalDavAccount?

    fun writeCalDavAccount(account: CalDavAccount)
}

class LocalEditionRepository(
    private val store: EditionStore,
    private val today: () -> LocalDate = { todayLocalDate() },
    private val forceSampleLoadFailure: () -> Boolean = { false },
    private val generateSample: (LocalDate) -> Edition = SampleGenerator::forDay,
    private val readFeed: suspend (String) -> String = { error(FEED_DIDNT_LOAD) },
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
    private val writesDeviceCalendarSnapshot: Boolean = false,
    private val readDeviceCalendar: suspend (LocalDate) -> DeviceCalendarRead = { DeviceCalendarRead.Unavailable },
    private val readCalDav: suspend (CalDavAccount) -> String = { error(CALDAV_DIDNT_LOAD) },
    private val serverConfigured: Boolean = false,
) : EditionRepository {
    override suspend fun loadCurrentMorningEdition(): Result<Edition> = runCatching {
        val dayKey = today().toDayKey()
        store.loadMorningEdition(dayKey)?.let { return@runCatching it }

        if (store.readSourceList() != null) {
            error(MORNING_EDITION_NOT_READY)
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

    override fun savedCalendarUrls(): List<String> = store.readSourceList()?.calendars.orEmpty()

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

    override suspend fun saveCalendarUrl(url: String): Result<Unit> = runCatching {
        val trimmed = url.trim()
        if (trimmed.isEmpty() || calendarUrlSendsPassword(trimmed)) error(CALENDAR_LINK_DIDNT_OPEN)
        val document = try {
            readFeed(trimmed)
        } catch (error: Throwable) {
            rethrowCancellation(error)
            error(CALENDAR_LINK_DIDNT_OPEN)
        }
        if (!document.contains("BEGIN:VCALENDAR", ignoreCase = true)) error(CALENDAR_LINK_DIDNT_OPEN)
        val current = store.readSourceList() ?: SourceList()
        if (current.calendars == listOf(trimmed)) return@runCatching
        store.writeSourceList(
            current.copy(
                id = current.id.ifBlank { "sources" },
                revision = current.revision + 1,
                feeds = current.feeds,
                calendars = listOf(trimmed),
            ),
        )
    }

    /**
     * Writes a non-empty Android read. Empty, failed, and unavailable reads leave the snapshot.
     * This never saves the morning edition.
     */
    suspend fun recordDeviceCalendar() {
        recordDeviceCalendar(today())
    }

    private suspend fun recordDeviceCalendar(day: LocalDate) {
        if (!writesDeviceCalendarSnapshot) return
        val read = try {
            readDeviceCalendar(day)
        } catch (error: Throwable) {
            rethrowCancellation(error)
            DeviceCalendarRead.Failed
        }
        if (read is DeviceCalendarRead.Events && read.occurrences.isNotEmpty()) {
            store.writeCalendarSnapshot(CalendarSnapshot(occurrences = read.occurrences))
        }
    }

    /**
     * Inserts today's morning edition when it is absent and no server is configured.
     * An existing morning document, including a saved sample, is left untouched and unread from sources.
     */
    suspend fun runMorning(): Result<Edition?> = runCatching {
        val date = today()
        if (store.loadMorningEdition(date.toDayKey()) != null || serverConfigured) {
            null
        } else {
            writeGeneratedToday(date, replaceExisting = false)
        }
    }

    override suspend fun generateToday(): Result<Edition> = runCatching {
        writeGeneratedToday(today(), replaceExisting = true)
    }

    private suspend fun writeGeneratedToday(date: LocalDate, replaceExisting: Boolean): Edition {
        val kept = if (replaceExisting) null else store.loadMorningEdition(date.toDayKey())
        if (kept != null) return kept
        val list = store.readSourceList()
        if (list == null || list.feeds.isEmpty()) {
            error(FEED_DIDNT_LOAD)
        }
        val parsed = list.feeds.map { url ->
            runCatching { parseFeedDocument(readDocument(url)) }
        }
        if (parsed.none { it.isSuccess }) error(FEED_DIDNT_LOAD)
        val arrived = if (replaceExisting) null else store.loadMorningEdition(date.toDayKey())
        if (arrived != null) return arrived
        recordDeviceCalendar(date)
        val calDav = readCalDavOccurrences()
        val events = editionEventsFrom(
            sources = listOf(
                readIcsOccurrences(list),
                store.readCalendarSnapshot()?.occurrences.orEmpty(),
                calDav.occurrences,
            ),
            day = date,
            zone = timeZone,
        )
        val existing = store.loadMorningEdition(date.toDayKey())
        if (!replaceExisting && existing != null) return existing
        val edition = EditionGenerator.fromFeedResults(
            date = date,
            parsed = parsed,
            existing = existing,
            topics = store.readSettings()?.topics.orEmpty(),
            events = events,
        )
        val noted = if (calDav.notes.isEmpty()) {
            edition
        } else {
            edition.copy(sourceNotes = edition.sourceNotes + calDav.notes)
        }
        if (!replaceExisting) {
            val current = store.loadMorningEdition(date.toDayKey())
            if (current != null) return current
        }
        store.saveMorningEdition(noted)
        return noted
    }

    override fun savedCalDavAccount(): CalDavAccount? =
        store.readCalDavAccount()?.copy(password = "")

    override suspend fun saveCalDavAccount(url: String, username: String, password: String): Result<Unit> = runCatching {
        val trimmedUrl = url.trim()
        val trimmedUser = username.trim()
        if (trimmedUrl.isEmpty() || trimmedUser.isEmpty()) error(CALDAV_DIDNT_LOAD)
        if (!trimmedUrl.startsWith("https://")) error(CALDAV_DIDNT_LOAD)
        if (calendarUrlSendsPassword(trimmedUrl)) error(CALDAV_DIDNT_LOAD)
        val current = store.readCalDavAccount()
        if (password.isEmpty()) {
            if (current != null && current.url == trimmedUrl && current.username == trimmedUser) return@runCatching
            error(CALDAV_DIDNT_LOAD)
        }
        val next = CalDavAccount(url = trimmedUrl, username = trimmedUser, password = password)
        if (current == next) return@runCatching
        store.writeCalDavAccount(next)
    }

    private suspend fun readCalDavOccurrences(): CalDavRead {
        return try {
            val account = store.readCalDavAccount() ?: return CalDavRead(emptyList(), emptyList())
            if (account.url.isBlank() || account.username.isBlank() || account.password.isBlank()) {
                return CalDavRead(emptyList(), listOf(CALDAV_DIDNT_LOAD))
            }
            CalDavRead(
                occurrences = occurrencesFromCalDavResponse(readCalDav(account), timeZone),
                notes = emptyList(),
            )
        } catch (error: Throwable) {
            rethrowCancellation(error)
            CalDavRead(emptyList(), listOf(CALDAV_DIDNT_LOAD))
        }
    }

    private suspend fun readIcsOccurrences(list: SourceList): List<CalendarOccurrence> {
        val url = list.calendars.firstOrNull { it.isNotBlank() } ?: return emptyList()
        if (calendarUrlSendsPassword(url)) return emptyList()
        return try {
            parseIcsOccurrences(readDocument(url), timeZone)
        } catch (error: Throwable) {
            rethrowCancellation(error)
            emptyList()
        }
    }

    private suspend fun readDocument(url: String): String =
        try {
            readFeed(url)
        } catch (error: Throwable) {
            rethrowCancellation(error)
            error(FEED_DIDNT_LOAD)
        }
}

private data class CalDavRead(
    val occurrences: List<CalendarOccurrence>,
    val notes: List<String>,
)

private fun rethrowCancellation(error: Throwable) {
    if (error is kotlin.coroutines.cancellation.CancellationException) throw error
}

interface EditionRepository {
    suspend fun loadCurrentMorningEdition(): Result<Edition>
    suspend fun retrySample(): Result<Edition>
    fun savedFeedUrls(): List<String>
    suspend fun saveFeedUrl(url: String): Result<Unit>
    fun savedCalendarUrls(): List<String>
    suspend fun saveCalendarUrl(url: String): Result<Unit>
    fun savedCalDavAccount(): CalDavAccount?
    suspend fun saveCalDavAccount(url: String, username: String, password: String): Result<Unit>
    fun savedTopics(): List<String>
    fun saveTopics(topics: List<String>)
    suspend fun generateToday(): Result<Edition>
}
