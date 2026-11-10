package hu.toliver.matutine

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit
import org.w3c.xhr.XMLHttpRequest

@OptIn(ExperimentalWasmJsInterop::class)
private suspend fun fetchGet(path: String, token: String): ApiResponse {
    val headers = Headers()
    headers.append("Authorization", "Bearer $token")
    val response = window.fetch(path, RequestInit(method = "GET", headers = headers)).await()
    val text = response.text().await<JsString>().toString()
    return ApiResponse(status = response.status.toInt(), body = text)
}

@OptIn(ExperimentalWasmJsInterop::class)
private suspend fun fetchWithBody(method: String, path: String, token: String, body: String): ApiResponse {
    val xhr = XMLHttpRequest()
    xhr.open(method, path, async = false)
    xhr.setRequestHeader("Authorization", "Bearer $token")
    xhr.setRequestHeader("Content-Type", "application/json")
    xhr.send(body)
    return ApiResponse(status = xhr.status.toInt(), body = xhr.responseText)
}

actual suspend fun apiGet(path: String, token: String): ApiResponse = fetchGet(path, token)

actual suspend fun apiPut(path: String, token: String, body: String): ApiResponse =
    fetchWithBody("PUT", path, token, body)

actual suspend fun apiPost(path: String, token: String, body: String): ApiResponse =
    fetchWithBody("POST", path, token, body)
