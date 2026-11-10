package hu.toliver.matutine

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.Promise
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response

@OptIn(ExperimentalWasmJsInterop::class)
actual suspend fun fetchCurrentEdition(token: String): EditionFetch {
    val headers = Headers()
    headers.append("Authorization", "Bearer $token")
    val pending: Promise<Response> = window.fetch(
        "/editions/current",
        RequestInit(method = "GET", headers = headers),
    )
    val response = pending.await()
    val body = response.text().await<JsString>().toString()
    return EditionFetch(status = response.status.toInt(), body = body)
}
