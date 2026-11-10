package hu.toliver.matutine

data class EditionFetch(
    val status: Int,
    val body: String,
)

expect suspend fun fetchCurrentEdition(token: String): EditionFetch
