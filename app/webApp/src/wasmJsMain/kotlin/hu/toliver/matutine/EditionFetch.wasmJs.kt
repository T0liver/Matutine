package hu.toliver.matutine

import org.w3c.xhr.XMLHttpRequest

actual suspend fun fetchCurrentEdition(token: String): EditionFetch {
    val xhr = XMLHttpRequest()
    xhr.open("GET", "/editions/current", async = false)
    xhr.setRequestHeader("Authorization", "Bearer $token")
    xhr.send()
    return EditionFetch(status = xhr.status.toInt(), body = xhr.responseText)
}