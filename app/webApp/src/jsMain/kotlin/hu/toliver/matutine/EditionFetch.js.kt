package hu.toliver.matutine

import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.Headers
import org.w3c.fetch.RequestInit

actual suspend fun fetchCurrentEdition(token: String): EditionFetch {
    val headers = Headers()
    headers.append("Authorization", "Bearer $token")
    val response = window.fetch(
        "/editions/current",
        RequestInit(method = "GET", headers = headers),
    ).await()
    return EditionFetch(status = response.status.toInt(), body = response.text().await())
}
