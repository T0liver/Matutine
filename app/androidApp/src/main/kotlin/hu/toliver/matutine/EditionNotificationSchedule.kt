package hu.toliver.matutine

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import hu.toliver.matutine.edition.decodeReaderSettings
import hu.toliver.matutine.edition.resolvedNotificationTime
import hu.toliver.matutine.store.FileEditionStore
import hu.toliver.matutine.store.androidDataRoot
import java.io.File
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

const val EDITION_NOTIFICATION_WORK_NAME = "matutine-edition-notification"

internal data class EditionNotificationWorkSchedule(
    val uniqueName: String,
    val policy: ExistingWorkPolicy,
    val request: androidx.work.OneTimeWorkRequest,
)

internal fun editionNotificationWorkSchedule(
    hour: Int,
    minute: Int,
    now: ZonedDateTime = ZonedDateTime.now(),
): EditionNotificationWorkSchedule {
    val delayMs = millisUntilNextNotification(hour, minute, now)
    val request = OneTimeWorkRequestBuilder<EditionNotificationWorker>()
        .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
        .build()
    return EditionNotificationWorkSchedule(
        uniqueName = EDITION_NOTIFICATION_WORK_NAME,
        policy = ExistingWorkPolicy.REPLACE,
        request = request,
    )
}

internal fun millisUntilNextNotification(
    hour: Int,
    minute: Int,
    now: ZonedDateTime,
): Long {
    var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
    if (!target.isAfter(now)) {
        target = target.plusDays(1)
    }
    return Duration.between(now, target).toMillis().coerceAtLeast(0L)
}

fun scheduleEditionNotification(context: Context) {
    val time = readSavedNotificationTime(context)
    val (hour, minute) = hu.toliver.matutine.edition.parseNotificationTime(time)
        ?: hu.toliver.matutine.edition.parseNotificationTime(
            hu.toliver.matutine.edition.DEFAULT_NOTIFICATION_TIME,
        )!!
    val schedule = editionNotificationWorkSchedule(hour, minute)
    WorkManager.getInstance(context).enqueueUniqueWork(
        schedule.uniqueName,
        schedule.policy,
        schedule.request,
    )
}

private fun readSavedNotificationTime(context: Context): String {
    val file = File(androidDataRoot(context), "settings.json")
    if (!file.exists()) return hu.toliver.matutine.edition.DEFAULT_NOTIFICATION_TIME
    return runCatching { decodeReaderSettings(file.readText()).resolvedNotificationTime() }
        .getOrDefault(hu.toliver.matutine.edition.DEFAULT_NOTIFICATION_TIME)
}
