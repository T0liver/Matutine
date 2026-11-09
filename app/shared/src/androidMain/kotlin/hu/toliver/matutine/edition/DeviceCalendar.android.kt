package hu.toliver.matutine.edition

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.provider.Settings
import hu.toliver.matutine.MatutineActivityHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.coroutines.cancellation.CancellationException

actual fun writesDeviceCalendarSnapshot(): Boolean = true

actual fun calendarPermissionIsOff(): Boolean {
    val context = MatutineActivityHolder.context ?: return false
    return context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED
}

actual fun openCalendarPermissionSettings() {
    val context = MatutineActivityHolder.context ?: return
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

actual suspend fun readDeviceCalendar(day: LocalDate): DeviceCalendarRead {
    val context = MatutineActivityHolder.context ?: return DeviceCalendarRead.Failed
    return readDeviceCalendar(context, day)
}

suspend fun readDeviceCalendar(context: Context, day: LocalDate): DeviceCalendarRead {
    if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
        return DeviceCalendarRead.Failed
    }
    val zone = TimeZone.currentSystemDefault()
    return withContext(Dispatchers.IO) {
        try {
            val start = day.atStartOfDayIn(zone).toEpochMilliseconds()
            val end = day.plus(1, DateTimeUnit.DAY).atStartOfDayIn(zone).toEpochMilliseconds()
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
                ContentUris.appendId(this, start)
                ContentUris.appendId(this, end)
            }.build()
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.ALL_DAY,
                ),
                null,
                null,
                null,
            ) ?: return@withContext DeviceCalendarRead.Failed
            cursor.use { rows ->
                val titleIndex = rows.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIndex = rows.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIndex = rows.getColumnIndex(CalendarContract.Instances.END)
                val allDayIndex = rows.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                if (titleIndex < 0 || beginIndex < 0) return@use DeviceCalendarRead.Failed
                val occurrences = mutableListOf<CalendarOccurrence>()
                while (rows.moveToNext()) {
                    val title = rows.getString(titleIndex)?.trim().orEmpty()
                    if (title.isEmpty()) continue
                    val beginMs = rows.getLong(beginIndex)
                    val endMs = if (endIndex >= 0 && !rows.isNull(endIndex)) rows.getLong(endIndex) else beginMs
                    val allDay = allDayIndex >= 0 && rows.getInt(allDayIndex) == 1
                    val occurrence = CalendarOccurrence(
                        title = title,
                        startEpochSecond = beginMs / 1000,
                        endEpochSecond = endMs / 1000,
                        allDay = allDay,
                    )
                    if (occurrence.startsOn(day, zone)) occurrences += occurrence
                }
                if (occurrences.isEmpty()) DeviceCalendarRead.Empty else DeviceCalendarRead.Events(occurrences)
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            DeviceCalendarRead.Failed
        }
    }
}
