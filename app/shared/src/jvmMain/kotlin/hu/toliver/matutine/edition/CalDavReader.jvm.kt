package hu.toliver.matutine.edition

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val calDavClient: HttpClient = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NEVER)
    .connectTimeout(Duration.ofSeconds(20))
    .build()

actual suspend fun readCalDavCalendar(account: CalDavAccount): String = withContext(Dispatchers.IO) {
    val url = account.url.trim()
    val uri = URI.create(url)
    if (!uri.userInfo.isNullOrEmpty()) error(CALDAV_DIDNT_LOAD)
    val request = HttpRequest.newBuilder()
        .uri(uri)
        .timeout(Duration.ofSeconds(20))
        .header("Authorization", basicAuthorizationValue(account.username, account.password))
        .header("Depth", "1")
        .header("Content-Type", "application/xml; charset=utf-8")
        .header("User-Agent", "Matutine")
        .method("REPORT", HttpRequest.BodyPublishers.ofString(calDavReportDocument()))
        .build()
    val response = calDavClient.send(request, HttpResponse.BodyHandlers.ofString())
    val code = response.statusCode()
    if (code != 207 && code !in 200..299) error(CALDAV_DIDNT_LOAD)
    response.body()
}
