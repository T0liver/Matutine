package hu.toliver.matutine.edition

actual suspend fun readFeedDocument(@Suppress("UNUSED_PARAMETER") url: String): String {
    error(FEED_DIDNT_LOAD)
}
