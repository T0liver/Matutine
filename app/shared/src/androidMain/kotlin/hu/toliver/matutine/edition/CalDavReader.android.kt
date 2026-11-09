package hu.toliver.matutine.edition

import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun readCalDavCalendar(account: CalDavAccount): String = withContext(Dispatchers.IO) {
    val url = account.url.trim()
    val uri = URI.create(url)
    if (!uri.userInfo.isNullOrEmpty()) error(CALDAV_DIDNT_LOAD)
    val connection = (uri.toURL().openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = false
        connectTimeout = 20_000
        readTimeout = 20_000
        doOutput = true
        setRequestProperty("Authorization", basicAuthorizationValue(account.username, account.password))
        setRequestProperty("Depth", "1")
        setRequestProperty("Content-Type", "application/xml; charset=utf-8")
        setRequestProperty("User-Agent", "Matutine")
    }
    useReportMethod(connection)
    try {
        connection.outputStream.use { stream ->
            stream.write(calDavReportDocument().encodeToByteArray())
        }
        val code = connection.responseCode
        if (code != 207 && code !in 200..299) error(CALDAV_DIDNT_LOAD)
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

private fun useReportMethod(connection: HttpURLConnection) {
    var target: Any = connection
    while (true) {
        val delegate = target.javaClass.declaredFields.firstOrNull { it.name == "delegate" } ?: break
        delegate.isAccessible = true
        target = delegate.get(target) ?: break
    }
    val method = generateSequence<Class<*>>(target.javaClass) { it.superclass }
        .mapNotNull { clazz -> runCatching { clazz.getDeclaredField("method") }.getOrNull() }
        .firstOrNull() ?: error(CALDAV_DIDNT_LOAD)
    method.isAccessible = true
    method.set(target, "REPORT")
}
