package hu.toliver.matutine.edition

import kotlinx.serialization.Serializable

const val TOKEN_DOES_NOT_MATCH = "That token doesn't match."
const val SERVER_COPY_ON_DEVICE = "Couldn't reach the server. Showing the copy on this device."
const val SERVER_ADDRESS_NEEDS_HTTP = "Use an http or https address."
const val UNSET_BEARER_TOKEN = "matutine-demo"

@Serializable
data class ServerLink(
    val baseUrl: String = "",
    val token: String = "",
)

fun presentedBearerToken(raw: String): String = raw.trim().ifEmpty { UNSET_BEARER_TOKEN }

fun normalizeServerBaseUrl(raw: String): String? {
    val trimmed = raw.trim().trimEnd('/')
    val scheme = when {
        trimmed.startsWith("https://") -> "https://"
        trimmed.startsWith("http://") -> "http://"
        else -> return null
    }
    val rest = trimmed.removePrefix(scheme)
    if (rest.isEmpty() || rest.any { it.isWhitespace() }) return null
    return trimmed
}

fun encodeServerLink(link: ServerLink): String =
    editionJson.encodeToString(ServerLink.serializer(), link)

fun decodeServerLink(json: String): ServerLink =
    editionJson.decodeFromString(ServerLink.serializer(), json)
