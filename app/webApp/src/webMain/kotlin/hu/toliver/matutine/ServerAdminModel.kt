package hu.toliver.matutine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import hu.toliver.matutine.edition.CALENDAR_LINK_DIDNT_OPEN
import hu.toliver.matutine.edition.CalDavAccount
import hu.toliver.matutine.edition.DEFAULT_NOTIFICATION_TIME
import hu.toliver.matutine.edition.Edition
import hu.toliver.matutine.edition.FEED_DIDNT_LOAD
import hu.toliver.matutine.edition.ReaderSettings
import hu.toliver.matutine.edition.ServerProcessConfig
import hu.toliver.matutine.edition.SourceList
import hu.toliver.matutine.edition.decodeCalDavAccount
import hu.toliver.matutine.edition.decodeEdition
import hu.toliver.matutine.edition.decodeReaderSettings
import hu.toliver.matutine.edition.decodeServerProcessConfig
import hu.toliver.matutine.edition.decodeSourceList
import hu.toliver.matutine.edition.encodeCalDavAccount
import hu.toliver.matutine.edition.encodeReaderSettings
import hu.toliver.matutine.edition.encodeServerProcessConfig
import hu.toliver.matutine.edition.encodeSourceList

class ServerAdminModel(
  val bearerToken: String,
) {
    var feedUrl by mutableStateOf("")
        private set
    var feedFieldError by mutableStateOf<String?>(null)
        private set
    var calendarUrl by mutableStateOf("")
        private set
    var calendarFieldError by mutableStateOf<String?>(null)
        private set
    var calDavUrl by mutableStateOf("")
        private set
    var calDavUsername by mutableStateOf("")
        private set
    var calDavPassword by mutableStateOf("")
        private set
    var calDavFieldError by mutableStateOf<String?>(null)
        private set
    var generationTime by mutableStateOf(DEFAULT_NOTIFICATION_TIME)
        private set
    var generationTimeError by mutableStateOf<String?>(null)
        private set
    var timezone by mutableStateOf("")
        private set
    var aiProvider by mutableStateOf("gemini")
        private set
    var geminiKey by mutableStateOf("")
        private set
    var sharedToken by mutableStateOf("")
        private set
    var topics by mutableStateOf<List<String>>(emptyList())
        private set
    var generating by mutableStateOf(false)
        private set

    private var feedBaseline = ""
    private var calendarBaseline = ""
    private var calDavUrlBaseline = ""
    private var calDavUsernameBaseline = ""
    private var calDavPasswordBaseline = ""

    suspend fun load() {
        val settings = apiGet("/settings", bearerToken).let { response ->
            if (response.status == 200) decodeReaderSettings(response.body) else ReaderSettings()
        }
        topics = settings.topics
        geminiKey = settings.geminiKey
        val sources = apiGet("/sources", bearerToken)
        if (sources.status == 200) {
            val list = decodeSourceList(sources.body)
            feedBaseline = list.feeds.firstOrNull().orEmpty()
            calendarBaseline = list.calendars.firstOrNull().orEmpty()
        } else {
            feedBaseline = ""
            calendarBaseline = ""
        }
        feedUrl = feedBaseline
        calendarUrl = calendarBaseline
        val calDav = apiGet("/caldav", bearerToken)
        if (calDav.status == 200) {
            val account = decodeCalDavAccount(calDav.body)
            calDavUrlBaseline = account.url
            calDavUsernameBaseline = account.username
            calDavPasswordBaseline = ""
            calDavUrl = account.url
            calDavUsername = account.username
            calDavPassword = ""
        }
        val process = apiGet("/process", bearerToken)
        if (process.status == 200) {
            val config = decodeServerProcessConfig(process.body)
            generationTime = config.generationTime.ifBlank { DEFAULT_NOTIFICATION_TIME }
            timezone = config.timezone
            aiProvider = config.aiProvider.ifBlank { "gemini" }
            sharedToken = config.sharedToken
        }
    }

    fun updateFeedUrl(value: String) {
        if (value != feedUrl) feedFieldError = null
        feedUrl = value
    }

    fun updateCalendarUrl(value: String) {
        if (value != calendarUrl) calendarFieldError = null
        calendarUrl = value
    }

    fun updateCalDavUrl(value: String) {
        if (value != calDavUrl) calDavFieldError = null
        calDavUrl = value
    }

    fun updateCalDavUsername(value: String) {
        if (value != calDavUsername) calDavFieldError = null
        calDavUsername = value
    }

    fun updateCalDavPassword(value: String) {
        if (value != calDavPassword) calDavFieldError = null
        calDavPassword = value
    }

    fun updateGenerationTime(value: String) {
        if (value != generationTime) generationTimeError = null
        generationTime = value
    }

    fun updateTimezone(value: String) {
        timezone = value
    }

    fun updateGeminiKey(value: String) {
        geminiKey = value
    }

    fun updateSharedToken(value: String) {
        sharedToken = value
    }

    suspend fun saveFeed() {
        val typed = feedUrl.trim()
        val body = encodeSourceList(SourceList(feeds = listOf(typed)))
        val response = apiPut("/sources", bearerToken, body)
        if (response.status == 200) {
            feedBaseline = typed
            feedUrl = feedBaseline
            feedFieldError = null
        } else if (feedUrl == typed) {
            feedFieldError = if (response.body.contains(FEED_DIDNT_LOAD)) FEED_DIDNT_LOAD else FEED_DIDNT_LOAD
        }
    }

    suspend fun saveCalendar() {
        val typed = calendarUrl.trim()
        val feeds = feedBaseline.takeIf { it.isNotEmpty() }?.let { listOf(it) }.orEmpty()
        val existing = SourceList(feeds = feeds, calendars = listOf(typed))
        val response = apiPut("/sources", bearerToken, encodeSourceList(existing))
        if (response.status == 200) {
            calendarBaseline = typed
            calendarUrl = calendarBaseline
            calendarFieldError = null
        } else if (calendarUrl == typed) {
            calendarFieldError = CALENDAR_LINK_DIDNT_OPEN
        }
    }

    suspend fun saveCalDav() {
        val body = encodeCalDavAccount(
            CalDavAccount(
                url = calDavUrl.trim(),
                username = calDavUsername.trim(),
                password = calDavPassword,
            ),
        )
        val response = apiPut("/caldav", bearerToken, body)
        if (response.status == 200) {
            val account = decodeCalDavAccount(response.body)
            calDavUrlBaseline = account.url
            calDavUsernameBaseline = account.username
            calDavUrl = account.url
            calDavUsername = account.username
            calDavPassword = ""
            calDavFieldError = null
        } else {
            calDavFieldError = CALENDAR_LINK_DIDNT_OPEN
        }
    }

    suspend fun saveGenerationTime() {
        val response = apiPut(
            "/process",
            bearerToken,
            encodeServerProcessConfig(
                ServerProcessConfig(
                    sharedToken = sharedToken,
                    timezone = timezone,
                    generationTime = generationTime,
                    aiProvider = aiProvider,
                ),
            ),
        )
        if (response.status == 200) {
            generationTimeError = null
        } else {
            generationTimeError = "Use a time like 07:00."
        }
    }

    suspend fun saveTimezone() {
        apiPut(
            "/process",
            bearerToken,
            encodeServerProcessConfig(
                ServerProcessConfig(
                    sharedToken = sharedToken,
                    timezone = timezone.trim(),
                    generationTime = generationTime,
                    aiProvider = aiProvider,
                ),
            ),
        )
    }

    suspend fun saveGeminiKey() {
        val settings = ReaderSettings(topics = topics, geminiKey = geminiKey.trim())
        apiPut("/settings", bearerToken, encodeReaderSettings(settings))
    }

    suspend fun saveSharedToken() {
        apiPut(
            "/process",
            bearerToken,
            encodeServerProcessConfig(
                ServerProcessConfig(
                    sharedToken = sharedToken.trim(),
                    timezone = timezone,
                    generationTime = generationTime,
                    aiProvider = aiProvider,
                ),
            ),
        )
    }

    suspend fun saveTopics(values: List<String>): Boolean {
        topics = values
        val settings = ReaderSettings(topics = values, geminiKey = geminiKey.trim())
        return apiPut("/settings", bearerToken, encodeReaderSettings(settings)).status == 200
    }

    suspend fun generateEdition(): Edition? {
        generating = true
        return try {
            if (feedUrl.trim() != feedBaseline) saveFeed()
            if (calendarUrl.trim() != calendarBaseline) saveCalendar()
            if (calDavDirty()) saveCalDav()
            val response = apiPost("/editions/morning/regenerate", bearerToken)
            if (response.status == 200) decodeEdition(response.body) else null
        } finally {
            generating = false
        }
    }

    private fun calDavDirty(): Boolean =
        calDavUrl.trim() != calDavUrlBaseline ||
            calDavUsername.trim() != calDavUsernameBaseline ||
            calDavPassword.isNotEmpty()
}
