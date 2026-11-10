package hu.toliver.matutine

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import hu.toliver.matutine.edition.EDITION_READY_NOTIFICATION_BODY
import hu.toliver.matutine.edition.resolvedNotificationBody
import hu.toliver.matutine.edition.todayLocalDate
import hu.toliver.matutine.edition.toDayKey
import hu.toliver.matutine.store.FileEditionStore
import hu.toliver.matutine.store.androidDataRoot

class EditionNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): ListenableWorker.Result {
        val context = applicationContext
        if (notificationsMayPost(context)) {
            val store = FileEditionStore(androidDataRoot(context))
            val dayKey = todayLocalDate().toDayKey()
            if (store.loadMorningEdition(dayKey) != null) {
                val body = store.readSettings()?.resolvedNotificationBody() ?: EDITION_READY_NOTIFICATION_BODY
                postEditionReadyNotification(context, body)
            }
        }
        scheduleEditionNotification(context)
        return ListenableWorker.Result.success()
    }
}
