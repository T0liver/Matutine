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
    fun loadExtras(dayKey: String): List<Edition>
    fun saveExtra(edition: Edition)

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
    private val summarize: suspend (List<EditionStory>, String) -> String = { _, _ -> error("summarizer_unavailable") },
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

    override fun savedNotificationTime(): String =
        store.readSettings()?.resolvedNotificationTime() ?: DEFAULT_NOTIFICATION_TIME

    override fun savedNotificationBody(): String =
        store.readSettings()?.resolvedNotificationBody() ?: EDITION_READY_NOTIFICATION_BODY

    override fun savedEditionTheme(): String =
        store.readSettings()?.resolvedEditionTheme() ?: DEFAULT_EDITION_THEME

    override fun savedEditionLayout(): String =
        store.readSettings()?.resolvedEditionLayout() ?: DEFAULT_EDITION_LAYOUT

    override fun saveTopics(topics: List<String>) {
        val current = store.readSettings() ?: ReaderSettings()
        store.writeSettings(
            current.copy(
                topics = topics.map { it.trim() }.filter { it.isNotEmpty() },
            ),
        )
    }

    override fun saveNotificationTime(time: String): Result<Unit> = runCatching {
        val trimmed = time.trim()
        val parsed = parseNotificationTime(trimmed) ?: error("invalid_notification_time")
        val normalized = notificationTimeLabel(parsed.first, parsed.second)
        val current = store.readSettings() ?: ReaderSettings()
        if (current.notificationTime == normalized) {
            return@runCatching
        }
        store.writeSettings(current.copy(notificationTime = normalized))
    }

    override fun saveNotificationBody(body: String): Result<Unit> = runCatching {
        val normalized = body.trim().ifEmpty { EDITION_READY_NOTIFICATION_BODY }
        val current = store.readSettings() ?: ReaderSettings()
        if (current.resolvedNotificationBody() == normalized) {
            return@runCatching
        }
        store.writeSettings(current.copy(notificationBody = normalized))
    }

    override fun saveEditionTheme(themeId: String): Result<Unit> = runCatching {
        val normalized = normalizeEditionThemeId(themeId) ?: error("invalid_edition_theme")
        val current = store.readSettings() ?: ReaderSettings()
        if (current.resolvedEditionTheme() == normalized) {
            return@runCatching
        }
        store.writeSettings(current.copy(editionTheme = normalized))
    }

    override fun savedGeminiKey(): String = store.readSettings()?.geminiKey?.trim().orEmpty()

    override fun saveGeminiKey(key: String): Result<Unit> = runCatching {
        val normalized = key.trim()
        val current = store.readSettings() ?: ReaderSettings()
        if (current.geminiKey == normalized) return@runCatching
        store.writeSettings(current.copy(geminiKey = normalized))
    }

    override fun saveEditionLayout(layoutId: String): Result<Unit> = runCatching {
        val normalized = normalizeEditionLayoutId(layoutId) ?: error("invalid_edition_layout")
        val current = store.readSettings() ?: ReaderSettings()
        if (current.resolvedEditionLayout() == normalized) {
            return@runCatching
        }
        store.writeSettings(current.copy(editionLayout = normalized))
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

    override suspend fun updateMorningEdition(edition: Edition): Result<Edition> = runCatching {
        val dayKey = today().toDayKey()
        require(edition.dayKey == dayKey && edition.role == EditionRole.morning)
        val stored = store.loadMorningEdition(dayKey) ?: error(MORNING_EDITION_NOT_READY)
        require(stored.id == edition.id)
        val next = edition.copy(revision = stored.revision + 1)
        store.saveMorningEdition(next)
        next
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
        val next = if (replaceExisting && existing != null && !existing.isSample) {
            keepMorningEdits(existing, noted)
        } else {
            withModelBriefing(noted)
        }
        if (!replaceExisting) {
            val current = store.loadMorningEdition(date.toDayKey())
            if (current != null) return current
        }
        store.saveMorningEdition(next)
        return next
    }

    override suspend fun newEditionNow(): Result<Edition> = runCatching {
        val date = today()
        val dayKey = date.toDayKey()
        if (store.loadMorningEdition(dayKey) == null) error(MORNING_EDITION_NOT_READY)
        val list = store.readSourceList()
        if (list == null || list.feeds.isEmpty()) error(FEED_DIDNT_LOAD)
        val parsed = list.feeds.map { url ->
            runCatching { parseFeedDocument(readDocument(url)) }
        }
        if (parsed.none { it.isSuccess }) error(FEED_DIDNT_LOAD)
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
        val edition = EditionGenerator.fromFeedResults(
            date = date,
            parsed = parsed,
            existing = null,
            topics = store.readSettings()?.topics.orEmpty(),
            events = events,
        )
        val noted = if (calDav.notes.isEmpty()) {
            edition
        } else {
            edition.copy(sourceNotes = edition.sourceNotes + calDav.notes)
        }
        val extra = withModelBriefing(noted).copy(
            id = nextExtraId(dayKey),
            revision = 1,
            role = EditionRole.extra,
        )
        store.saveExtra(extra)
        extra
    }

    override suspend fun loadSameDayExtras(): List<Edition> =
        store.loadExtras(today().toDayKey())

    override suspend fun updateExtra(edition: Edition): Result<Edition> = runCatching {
        val dayKey = today().toDayKey()
        require(edition.dayKey == dayKey && edition.role == EditionRole.extra)
        val stored = store.loadExtras(dayKey).singleOrNull { it.id == edition.id }
            ?: error(MORNING_EDITION_NOT_READY)
        val next = edition.copy(
            id = stored.id,
            revision = stored.revision + 1,
            dayKey = stored.dayKey,
            role = EditionRole.extra,
        )
        store.saveExtra(next)
        next
    }

    private fun nextExtraId(dayKey: String): String {
        val prefix = "extra-$dayKey-"
        val used = store.loadExtras(dayKey).mapNotNull { extra ->
            extra.id.removePrefix(prefix).toIntOrNull()
        }
        return prefix + ((used.maxOrNull() ?: 0) + 1)
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

    private suspend fun withModelBriefing(edition: Edition): Edition {
        val key = savedGeminiKey()
        if (key.isEmpty() || edition.stories.isEmpty()) return edition
        val text = try {
            summarize(edition.stories, key).trim()
        } catch (error: Throwable) {
            rethrowCancellation(error)
            return edition
        }
        if (text.isEmpty()) return edition
        return edition.copy(briefing = text)
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
    fun savedNotificationTime(): String
    fun saveNotificationTime(time: String): Result<Unit>
    fun savedNotificationBody(): String
    fun saveNotificationBody(body: String): Result<Unit>
    fun savedEditionTheme(): String
    fun saveEditionTheme(themeId: String): Result<Unit>
    fun savedEditionLayout(): String
    fun saveEditionLayout(layoutId: String): Result<Unit>
    fun savedGeminiKey(): String
    fun saveGeminiKey(key: String): Result<Unit>
    suspend fun generateToday(): Result<Edition>
    suspend fun newEditionNow(): Result<Edition>
    suspend fun loadSameDayExtras(): List<Edition>
    suspend fun updateMorningEdition(edition: Edition): Result<Edition>
    suspend fun updateExtra(edition: Edition): Result<Edition>
}

internal fun keepMorningEdits(existing: Edition, generated: Edition): Edition {
    val freshKeys = generated.events.map { it.title to it.timeLabel }.toSet()
    val keptLines = existing.events.filter { (it.title to it.timeLabel) !in freshKeys }
    return generated.copy(
        briefing = existing.briefing,
        events = generated.events + keptLines,
        tasks = existing.tasks,
    )
}
