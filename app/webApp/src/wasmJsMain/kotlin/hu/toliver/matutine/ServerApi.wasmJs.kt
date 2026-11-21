package hu.toliver.matutine

import org.w3c.xhr.XMLHttpRequest

private suspend fun fetchGet(path: String, token: String): ApiResponse {
    val xhr = XMLHttpRequest()
    xhr.open("GET", path, async = false)
    xhr.setRequestHeader("Authorization", "Bearer $token")
    xhr.send()
    return ApiResponse(status = xhr.status.toInt(), body = xhr.responseText)
}

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
