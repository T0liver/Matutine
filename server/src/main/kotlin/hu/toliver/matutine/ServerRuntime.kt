package hu.toliver.matutine

import hu.toliver.matutine.edition.Edition
import hu.toliver.matutine.edition.EditionStore
import hu.toliver.matutine.edition.EditionStory
import hu.toliver.matutine.edition.LocalEditionRepository
import hu.toliver.matutine.edition.ServerProcessConfig
import hu.toliver.matutine.edition.toDayKey
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.util.AttributeKey
import kotlin.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal val ServerRuntimeKey = AttributeKey<ServerRuntime>("matutine.runtime")

internal class ServerRuntime(
    val documents: EditionStore,
    private val application: Application,
    private val envToken: String,
    private val envZone: String?,
    private val envGeneration: String?,
    private val envGeminiKey: String,
    private val clock: () -> Instant,
    private val readFeed: suspend (String) -> String,
    private val summarize: suspend (List<EditionStory>, String) -> String,
) {
    var sharedToken: String = resolveEffectiveToken()
        private set

    var zone: TimeZone = resolveEffectiveZone()
        private set

    private var generation: Pair<Int, Int>? = resolveEffectiveGeneration()
    private var clockJob: Job? = null

    private var repository: LocalEditionRepository = buildRepository()

    init {
        application.monitor.subscribe(ApplicationStopped) { clockJob?.cancel() }
        restartClock()
    }

    fun repository(): LocalEditionRepository = repository

    suspend fun listTodayEditions(): List<Edition> {
        val dayKey = clock().toLocalDateTime(zone).date.toDayKey()
        val morning = documents.loadMorningEdition(dayKey)
        return listOfNotNull(morning) + repository.loadSameDayExtras()
    }

    fun reloadRepository() {
        repository = buildRepository()
    }

    fun effectiveProcessConfig(): ServerProcessConfig {
        val stored = documents.readProcessConfig()
        val zoneLabel = stored?.timezone?.takeIf { it.isNotBlank() }
            ?: envZone?.takeIf { it.isNotBlank() }
            ?: TimeZone.currentSystemDefault().id
        return ServerProcessConfig(
            sharedToken = sharedToken,
            timezone = zoneLabel,
            generationTime = generation?.let { (h, m) ->
                "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
            } ?: DEFAULT_GENERATION_LABEL,
            aiProvider = stored?.aiProvider?.takeIf { it.isNotBlank() } ?: DEFAULT_AI_PROVIDER,
        )
    }

    fun applyProcessConfig(incoming: ServerProcessConfig): Result<Unit> = runCatching {
        val zoneText = incoming.timezone.trim()
        if (zoneText.isNotEmpty()) {
            TimeZone.of(zoneText)
        }
        val generationText = incoming.generationTime.trim()
        if (generationText.isNotEmpty() && resolveGenerationTime(generationText) == null) {
            error("invalid_generation_time")
        }
        val merged = ServerProcessConfig(
            sharedToken = incoming.sharedToken.trim(),
            timezone = zoneText,
            generationTime = generationText,
            aiProvider = DEFAULT_AI_PROVIDER,
        )
        documents.writeProcessConfig(merged)
        sharedToken = resolveEffectiveToken()
        zone = resolveEffectiveZone()
        generation = resolveEffectiveGeneration()
        repository = buildRepository()
        restartClock()
    }

    private fun resolveEffectiveToken(): String {
        val stored = documents.readProcessConfig()?.sharedToken?.trim().orEmpty()
        if (stored.isNotEmpty()) return stored
        return resolveSharedToken(envToken)
    }

    private fun resolveEffectiveZone(): TimeZone =
        resolveServerTimeZone(documents.readProcessConfig()?.timezone?.takeIf { it.isNotBlank() } ?: envZone)

    private fun resolveEffectiveGeneration(): Pair<Int, Int>? =
        resolveGenerationTime(
            documents.readProcessConfig()?.generationTime?.takeIf { it.isNotBlank() } ?: envGeneration,
        )

    private fun effectiveGeminiKey(): String {
        val env = envGeminiKey.trim()
        if (env.isNotEmpty()) return env
        return documents.readSettings()?.geminiKey?.trim().orEmpty()
    }

    private fun buildRepository(): LocalEditionRepository =
        serverEditionRepository(
            documents = documents,
            zone = zone,
            clock = clock,
            geminiKey = effectiveGeminiKey(),
            readFeed = readFeed,
            summarize = summarize,
        )

    private fun restartClock() {
        clockJob?.cancel()
        val (hour, minute) = generation ?: return
        val morning = ServerMorningClock(
            zone = zone,
            hour = hour,
            minute = minute,
            clock = clock,
            insert = { insertScheduledMorning(documents, repository) },
        )
        clockJob = application.launch { morning.run() }
    }

    private companion object {
        const val DEFAULT_GENERATION_LABEL = "07:00"
        const val DEFAULT_AI_PROVIDER = hu.toliver.matutine.edition.DEFAULT_AI_PROVIDER
    }
}
