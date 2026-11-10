package hu.toliver.matutine

import hu.toliver.matutine.edition.EditionStore
import hu.toliver.matutine.edition.EditionStory
import hu.toliver.matutine.edition.encodeEdition
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.http.content.staticResources
import io.ktor.util.AttributeKey
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

private val SharedTokenKey = AttributeKey<String>("matutine.sharedToken")
private val DocumentStoreKey = AttributeKey<EditionStore>("matutine.documents")
private val ZoneKey = AttributeKey<TimeZone>("matutine.zone")
private val GenerationRawKey = AttributeKey<String>("matutine.generationTime")
private val GeminiKeyKey = AttributeKey<String>("matutine.geminiKey")
private val ClockKey = AttributeKey<() -> Instant>("matutine.clock")
private val PortsKey = AttributeKey<ServerPorts>("matutine.ports")

internal class ServerPorts(
    val readFeed: (suspend (String) -> String)? = null,
    val summarize: (suspend (List<EditionStory>, String) -> String)? = null,
)

fun resolveSharedToken(raw: String?): String = raw?.takeIf { it.isNotBlank() } ?: "matutine-demo"

fun resolveDataRoot(raw: String?): String = raw?.takeIf { it.isNotBlank() } ?: "/data"

internal fun bearerMatches(authorization: String?, token: String): Boolean {
    val prefix = "Bearer "
    if (authorization == null || !authorization.startsWith(prefix)) return false
    val presented = authorization.substring(prefix.length)
    return presented.isNotEmpty() && presented == token
}

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val token = attributes.getOrNull(SharedTokenKey)
        ?: resolveSharedToken(System.getenv("MATUTINE_TOKEN"))
    val documents = attributes.getOrNull(DocumentStoreKey)
        ?: DataDirectoryStore(resolveDataRoot(System.getenv("MATUTINE_DATA")))
    val zone = attributes.getOrNull(ZoneKey)
        ?: resolveServerTimeZone(System.getenv("MATUTINE_TIMEZONE"))
    val generation = resolveGenerationTime(
        if (attributes.contains(GenerationRawKey)) {
            attributes[GenerationRawKey]
        } else {
            System.getenv("MATUTINE_GENERATION_TIME")
        },
    )
    val geminiKey = attributes.getOrNull(GeminiKeyKey) ?: System.getenv("MATUTINE_GEMINI_KEY").orEmpty()
    val clock = attributes.getOrNull(ClockKey) ?: { Clock.System.now() }
    val ports = attributes.getOrNull(PortsKey)
    val readFeed = ports?.readFeed ?: ::serverReadFeed
    val summarize = ports?.summarize ?: ::serverSummarize
    val repository = serverEditionRepository(
        documents = documents,
        zone = zone,
        clock = clock,
        geminiKey = geminiKey,
        readFeed = readFeed,
        summarize = summarize,
    )
    if (generation != null) {
        val (hour, minute) = generation
        val morning = ServerMorningClock(
            zone = zone,
            hour = hour,
            minute = minute,
            clock = clock,
            insert = { insertScheduledMorning(documents, repository) },
        )
        val job = launch { morning.run() }
        monitor.subscribe(ApplicationStopped) { job.cancel() }
    }
    routing {
        get("/editions/current") {
            val authorization = call.request.headers["Authorization"]
            if (!bearerMatches(authorization, token)) {
                call.respondText("unauthorized", status = HttpStatusCode.Unauthorized)
                return@get
            }
            val edition = repository.loadCurrentMorningEdition().getOrElse { error ->
                call.respondText(
                    error.message ?: "unavailable",
                    status = HttpStatusCode.NotFound,
                )
                return@get
            }
            call.respondText(encodeEdition(edition), ContentType.Application.Json)
        }
        staticResources("/", "web") {
            default("index.html")
        }
    }
}

fun Application.installTestDocuments(token: String, documents: EditionStore) {
    attributes.put(SharedTokenKey, token)
    attributes.put(DocumentStoreKey, documents)
}

fun Application.installServerClock(
    zone: TimeZone,
    generationTime: String,
    geminiKey: String = "",
    clock: () -> Instant,
    readFeed: (suspend (String) -> String)? = null,
    summarize: (suspend (List<EditionStory>, String) -> String)? = null,
) {
    attributes.put(ZoneKey, zone)
    attributes.put(GenerationRawKey, generationTime)
    attributes.put(GeminiKeyKey, geminiKey)
    attributes.put(ClockKey, clock)
    if (readFeed != null || summarize != null) {
        attributes.put(PortsKey, ServerPorts(readFeed, summarize))
    }
}
