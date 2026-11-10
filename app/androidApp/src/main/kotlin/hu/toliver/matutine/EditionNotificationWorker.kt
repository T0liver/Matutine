package hu.toliver.matutine

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import hu.toliver.matutine.edition.EDITION_READY_NOTIFICATION_BODY
import hu.toliver.matutine.edition.openRemoteTransport
import hu.toliver.matutine.edition.presentedBearerToken
import hu.toliver.matutine.edition.resolvedNotificationBody
import hu.toliver.matutine.edition.todayLocalDate
import hu.toliver.matutine.edition.toDayKey
import hu.toliver.matutine.settings.readDeviceServerLink
import hu.toliver.matutine.store.FileEditionStore
import hu.toliver.matutine.store.androidDataRoot

class EditionNotificationWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): ListenableWorker.Result {
        val context = applicationContext
        val root = androidDataRoot(context)
        val store = FileEditionStore(root)
        val link = readDeviceServerLink(root)
        val morningReturned = if (link == null) {
            store.loadMorningEdition(todayLocalDate().toDayKey()) != null
        } else {
            val status = runCatching {
                openRemoteTransport(link.baseUrl).exchange(
                    method = "GET",
                    path = "/editions/current",
                    token = presentedBearerToken(link.token),
                ).status
            }.getOrNull()
            morningEditionWasReturned(status)
        }
        if (shouldPostEditionNotification(notificationsMayPost(context), morningReturned)) {
            val body = store.readSettings()?.resolvedNotificationBody() ?: EDITION_READY_NOTIFICATION_BODY
            postEditionReadyNotification(context, body)
        }
        scheduleEditionNotification(context)
        return ListenableWorker.Result.success()
    }
}

internal fun morningEditionWasReturned(httpStatus: Int?): Boolean = httpStatus == 200

internal fun shouldPostEditionNotification(notificationsAllowed: Boolean, morningReturned: Boolean): Boolean =
    notificationsAllowed && morningReturned
