package hu.toliver.matutine

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import kotlin.js.Promise

private suspend fun fetchApi(method: String, path: String, token: String, body: String?): ApiResponse {
    val headers = Headers()
    headers.append("Authorization", "Bearer $token")
    if (body != null) headers.append("Content-Type", "application/json")
    val init = RequestInit(method = method, headers = headers)
    if (body != null) init.body = body
    val response = window.fetch(path, init).await<Response>()
    val text = response.text().await()
    return ApiResponse(status = response.status.toInt(), body = text)
}

actual suspend fun apiGet(path: String, token: String): ApiResponse = fetchApi("GET", path, token, null)

actual suspend fun apiPut(path: String, token: String, body: String): ApiResponse =
    fetchApi("PUT", path, token, body)

actual suspend fun apiPost(path: String, token: String, body: String): ApiResponse =
    fetchApi("POST", path, token, body.ifEmpty { null })
