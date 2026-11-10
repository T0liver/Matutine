package hu.toliver.matutine

import hu.toliver.matutine.edition.CALENDAR_LINK_DIDNT_OPEN
import hu.toliver.matutine.edition.DEFAULT_AI_PROVIDER
import hu.toliver.matutine.edition.FEED_DIDNT_LOAD
import hu.toliver.matutine.edition.ReaderSettings
import hu.toliver.matutine.edition.ServerProcessConfig
import hu.toliver.matutine.edition.SourceList
import hu.toliver.matutine.edition.EditionRole
import hu.toliver.matutine.edition.decodeEdition
import hu.toliver.matutine.edition.decodeCalDavAccount
import hu.toliver.matutine.edition.decodeReaderSettings
import hu.toliver.matutine.edition.decodeServerProcessConfig
import hu.toliver.matutine.edition.decodeSourceList
import hu.toliver.matutine.edition.encodeCalDavAccount
import hu.toliver.matutine.edition.encodeEdition
import hu.toliver.matutine.edition.encodeEditionList
import hu.toliver.matutine.edition.encodeReaderSettings
import hu.toliver.matutine.edition.encodeServerProcessConfig
import hu.toliver.matutine.edition.encodeSourceList
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

internal fun Route.configureMatutineApi(runtime: ServerRuntime) {
    route("/settings") {
        get {
            if (!authorize(call, runtime)) return@get
            val stored = runtime.documents.readSettings() ?: ReaderSettings()
            call.respondText(encodeReaderSettings(stored), ContentType.Application.Json)
        }
        put {
            if (!authorize(call, runtime)) return@put
            val incoming = decodeReaderSettings(call.receiveText())
            val current = runtime.documents.readSettings() ?: ReaderSettings()
            runtime.documents.writeSettings(
                current.copy(
                    topics = incoming.topics,
                    geminiKey = incoming.geminiKey.trim(),
                ),
            )
            runtime.reloadRepository()
            call.respondText(encodeReaderSettings(runtime.documents.readSettings() ?: ReaderSettings()))
        }
    }
    route("/process") {
        get {
            if (!authorize(call, runtime)) return@get
            call.respondText(encodeServerProcessConfig(runtime.effectiveProcessConfig()))
        }
        put {
            if (!authorize(call, runtime)) return@put
            val incoming = decodeServerProcessConfig(call.receiveText())
            val applied = runtime.applyProcessConfig(incoming.copy(aiProvider = DEFAULT_AI_PROVIDER))
            if (applied.isFailure) {
                val message = applied.exceptionOrNull()?.message ?: "bad_request"
                call.respondText(message, status = HttpStatusCode.BadRequest)
                return@put
            }
            call.respondText(encodeServerProcessConfig(runtime.effectiveProcessConfig()))
        }
    }
    route("/sources") {
        get {
            if (!authorize(call, runtime)) return@get
            val list = runtime.documents.readSourceList()
                ?: return@get call.respondText("absent", status = HttpStatusCode.NotFound)
            call.respondText(encodeSourceList(list), ContentType.Application.Json)
        }
        put {
            if (!authorize(call, runtime)) return@put
            val body = decodeSourceList(call.receiveText())
            val feed = body.feeds.firstOrNull()?.trim().orEmpty()
            val calendar = body.calendars.firstOrNull()?.trim().orEmpty()
            val repository = runtime.repository()
            if (feed.isNotEmpty()) {
                val saved = repository.saveFeedUrl(feed)
                if (saved.isFailure) {
                    call.respondText(saved.exceptionOrNull()?.message ?: FEED_DIDNT_LOAD, status = HttpStatusCode.BadRequest)
                    return@put
                }
            }
            if (calendar.isNotEmpty()) {
                val saved = repository.saveCalendarUrl(calendar)
                if (saved.isFailure) {
                    call.respondText(
                        saved.exceptionOrNull()?.message ?: CALENDAR_LINK_DIDNT_OPEN,
                        status = HttpStatusCode.BadRequest,
                    )
                    return@put
                }
            }
            val saved = runtime.documents.readSourceList()
                ?: SourceList(id = "sources", revision = 1)
            call.respondText(encodeSourceList(saved), ContentType.Application.Json)
        }
    }
    route("/caldav") {
        get {
            if (!authorize(call, runtime)) return@get
            val account = runtime.documents.readCalDavAccount()
                ?: return@get call.respondText("absent", status = HttpStatusCode.NotFound)
            call.respondText(encodeCalDavAccount(account.copy(password = "")))
        }
        put {
            if (!authorize(call, runtime)) return@put
            val incoming = decodeCalDavAccount(call.receiveText())
            val repository = runtime.repository()
            val saved = repository.saveCalDavAccount(incoming.url, incoming.username, incoming.password)
            if (saved.isFailure) {
                call.respondText(
                    saved.exceptionOrNull()?.message ?: CALENDAR_LINK_DIDNT_OPEN,
                    status = HttpStatusCode.BadRequest,
                )
                return@put
            }
            val stored = requireNotNull(runtime.documents.readCalDavAccount())
            call.respondText(encodeCalDavAccount(stored.copy(password = "")))
        }
    }
    put("/editions/by-id") {
        if (!authorize(call, runtime)) return@put
        val incoming = decodeEdition(call.receiveText())
        val saved = if (incoming.role == EditionRole.extra) {
            runtime.repository().updateExtra(incoming)
        } else {
            runtime.repository().updateMorningEdition(incoming)
        }
        if (saved.isFailure) {
            call.respondText(
                saved.exceptionOrNull()?.message ?: "bad_request",
                status = HttpStatusCode.BadRequest,
            )
            return@put
        }
        call.respondText(encodeEdition(saved.getOrThrow()), ContentType.Application.Json)
    }
    get("/editions") {
        if (!authorize(call, runtime)) return@get
        call.respondText(encodeEditionList(runtime.listTodayEditions()), ContentType.Application.Json)
    }
    post("/editions") {
        if (!authorize(call, runtime)) return@post
        val generated = runtime.repository().newEditionNow()
        if (generated.isFailure) {
            call.respondText(
                generated.exceptionOrNull()?.message ?: FEED_DIDNT_LOAD,
                status = HttpStatusCode.BadRequest,
            )
            return@post
        }
        call.respondText(encodeEdition(generated.getOrThrow()), ContentType.Application.Json)
    }
    post("/editions/morning/regenerate") {
        if (!authorize(call, runtime)) return@post
        val generated = runtime.repository().generateToday()
        if (generated.isFailure) {
            call.respondText(
                generated.exceptionOrNull()?.message ?: FEED_DIDNT_LOAD,
                status = HttpStatusCode.BadRequest,
            )
            return@post
        }
        call.respondText(encodeEdition(generated.getOrThrow()), ContentType.Application.Json)
    }
}

private suspend fun authorize(call: ApplicationCall, runtime: ServerRuntime): Boolean {
    val authorization = call.request.headers["Authorization"]
    if (!bearerMatches(authorization, runtime.sharedToken)) {
        call.respondText("unauthorized", status = HttpStatusCode.Unauthorized)
        return false
    }
    return true
}
