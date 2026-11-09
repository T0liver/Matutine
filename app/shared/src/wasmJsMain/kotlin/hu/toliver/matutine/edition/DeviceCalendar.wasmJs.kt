package hu.toliver.matutine.edition

import kotlinx.datetime.LocalDate

actual fun writesDeviceCalendarSnapshot(): Boolean = false

actual suspend fun readDeviceCalendar(@Suppress("UNUSED_PARAMETER") day: LocalDate): DeviceCalendarRead =
    DeviceCalendarRead.Unavailable

actual fun calendarPermissionIsOff(): Boolean = false

actual fun openCalendarPermissionSettings() = Unit
