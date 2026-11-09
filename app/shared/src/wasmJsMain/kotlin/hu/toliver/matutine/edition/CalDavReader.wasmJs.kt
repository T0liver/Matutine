package hu.toliver.matutine.edition

actual suspend fun readCalDavCalendar(@Suppress("UNUSED_PARAMETER") account: CalDavAccount): String {
    error(CALDAV_DIDNT_LOAD)
}
