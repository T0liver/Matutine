package hu.toliver.matutine.edition

import kotlinx.datetime.LocalDate

/** True only on Android, the only target that writes `calendar-snapshot.json`. */
expect fun writesDeviceCalendarSnapshot(): Boolean

/** Desktop, JS, and Wasm return [DeviceCalendarRead.Unavailable] and do not write. */
expect suspend fun readDeviceCalendar(day: LocalDate): DeviceCalendarRead

/** True when Android has denied READ_CALENDAR. Other targets are false. */
expect fun calendarPermissionIsOff(): Boolean

/** Opens the application details screen, where calendar permission is granted. */
expect fun openCalendarPermissionSettings()
