package hu.toliver.matutine.edition

import kotlinx.datetime.LocalDate

data class RemoteHttpResult(
    val status: Int,
    val body: String,
)

interface RemoteEditionTransport {
    suspend fun exchange(method: String, path: String, token: String, body: String? = null): RemoteHttpResult
}

/**
 * Reads and asks the server to regenerate or insert an extra.
 * Device settings stay on [local]. A failed connection shows the stored morning edition
 * and does not copy the server paper into the device store.
 */
class RemoteEditionRepository(
    private val local: EditionRepository,
    private val store: EditionStore,
    private val link: ServerLink,
    private val transport: RemoteEditionTransport,
    private val today: () -> LocalDate = { todayLocalDate() },
    private val writesEditions: Boolean = false,
) : EditionRepository {
    private var pendingNotice: String? = null
    private var showingStoredCopy = false

    override fun consumeLoadNotice(): String? {
        val notice = pendingNotice
        pendingNotice = null
        return notice
    }

    override suspend fun loadCurrentMorningEdition(): Result<Edition> {
        val response = call("GET", "/editions/current")
        if (response.isFailure) return storedCopy(SERVER_COPY_ON_DEVICE)
        val http = response.getOrThrow()
        return when (http.status) {
            200 -> {
                showingStoredCopy = false
                pendingNotice = null
                Result.success(decodeEdition(http.body))
            }
            401 -> storedCopy(TOKEN_DOES_NOT_MATCH)
            404 -> {
                showingStoredCopy = false
                pendingNotice = null
                Result.failure(IllegalStateException(MORNING_EDITION_NOT_READY))
            }
            else -> storedCopy(SERVER_COPY_ON_DEVICE)
        }
    }

    override suspend fun retrySample(): Result<Edition> =
        Result.failure(IllegalStateException(MORNING_EDITION_NOT_READY))

    override fun savedFeedUrls(): List<String> = local.savedFeedUrls()

    override suspend fun saveFeedUrl(url: String): Result<Unit> = local.saveFeedUrl(url)

    override fun savedCalendarUrls(): List<String> = local.savedCalendarUrls()

    override suspend fun saveCalendarUrl(url: String): Result<Unit> = local.saveCalendarUrl(url)

    override fun savedCalDavAccount(): CalDavAccount? = local.savedCalDavAccount()

    override suspend fun saveCalDavAccount(url: String, username: String, password: String): Result<Unit> =
        local.saveCalDavAccount(url, username, password)

    override fun savedTopics(): List<String> = local.savedTopics()

    override fun saveTopics(topics: List<String>) {
        local.saveTopics(topics)
    }

    override fun savedNotificationTime(): String = local.savedNotificationTime()

    override fun saveNotificationTime(time: String): Result<Unit> = local.saveNotificationTime(time)

    override fun savedNotificationBody(): String = local.savedNotificationBody()

    override fun saveNotificationBody(body: String): Result<Unit> = local.saveNotificationBody(body)

    override fun savedEditionTheme(): String = local.savedEditionTheme()

    override fun saveEditionTheme(themeId: String): Result<Unit> = local.saveEditionTheme(themeId)

    override fun savedEditionLayout(): String = local.savedEditionLayout()

    override fun saveEditionLayout(layoutId: String): Result<Unit> = local.saveEditionLayout(layoutId)

    override fun savedGeminiKey(): String = ""

    override fun saveGeminiKey(key: String): Result<Unit> = Result.success(Unit)

    override suspend fun generateToday(): Result<Edition> {
        val response = call("POST", "/editions/morning/regenerate")
        if (response.isFailure) return local.generateToday()
        val http = response.getOrThrow()
        return when (http.status) {
            200 -> {
                showingStoredCopy = false
                Result.success(decodeEdition(http.body))
            }
            401 -> Result.failure(IllegalStateException(TOKEN_DOES_NOT_MATCH))
            else -> Result.failure(IllegalStateException(http.body.ifBlank { FEED_DIDNT_LOAD }))
        }
    }

    override suspend fun newEditionNow(): Result<Edition> {
        val response = call("POST", "/editions")
        if (response.isFailure) return Result.failure(IllegalStateException(SERVER_COPY_ON_DEVICE))
        val http = response.getOrThrow()
        return when (http.status) {
            200 -> Result.success(decodeEdition(http.body))
            401 -> Result.failure(IllegalStateException(TOKEN_DOES_NOT_MATCH))
            else -> Result.failure(IllegalStateException(http.body.ifBlank { FEED_DIDNT_LOAD }))
        }
    }

    override suspend fun loadSameDayExtras(): List<Edition> {
        if (showingStoredCopy) return local.loadSameDayExtras()
        val response = call("GET", "/editions")
        val http = response.getOrNull() ?: return emptyList()
        if (http.status != 200) return emptyList()
        return decodeEditionList(http.body).filter { edition ->
            edition.role == EditionRole.extra && edition.dayKey == today().toDayKey()
        }
    }

    override suspend fun updateMorningEdition(edition: Edition): Result<Edition> = writeEdition(edition)

    override suspend fun updateExtra(edition: Edition): Result<Edition> = writeEdition(edition)

    private suspend fun writeEdition(edition: Edition): Result<Edition> {
        if (!writesEditions) return refusedEdit()
        val response = call("PUT", "/editions/by-id", encodeEdition(edition))
        if (response.isFailure) return Result.failure(IllegalStateException(EditionTranscript.SAVE_FAILED))
        val http = response.getOrThrow()
        return when (http.status) {
            200 -> Result.success(decodeEdition(http.body))
            401 -> Result.failure(IllegalStateException(TOKEN_DOES_NOT_MATCH))
            else -> Result.failure(
                IllegalStateException(http.body.ifBlank { EditionTranscript.SAVE_FAILED }),
            )
        }
    }

    private fun refusedEdit(): Result<Edition> =
        Result.failure(IllegalStateException("Android does not edit a server edition"))

    private fun storedCopy(message: String): Result<Edition> {
        pendingNotice = message
        val stored = store.loadMorningEdition(today().toDayKey())
        showingStoredCopy = stored != null
        return if (stored != null) {
            Result.success(stored)
        } else {
            Result.failure(IllegalStateException(message))
        }
    }

    private suspend fun call(method: String, path: String, body: String? = null): Result<RemoteHttpResult> =
        try {
            Result.success(
                transport.exchange(method, path, presentedBearerToken(link.token), body),
            )
        } catch (error: Throwable) {
            if (error is kotlin.coroutines.cancellation.CancellationException) throw error
            Result.failure(error)
        }
}
