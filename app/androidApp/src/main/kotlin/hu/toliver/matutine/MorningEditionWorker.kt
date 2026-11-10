package hu.toliver.matutine

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import hu.toliver.matutine.edition.LocalEditionRepository
import hu.toliver.matutine.edition.readCalDavCalendar
import hu.toliver.matutine.edition.readDeviceCalendar
import hu.toliver.matutine.edition.readFeedDocument
import hu.toliver.matutine.edition.summarizeWithGemini
import hu.toliver.matutine.edition.writesDeviceCalendarSnapshot
import hu.toliver.matutine.store.FileEditionStore
import hu.toliver.matutine.store.androidDataRoot
import java.util.concurrent.TimeUnit

private const val MORNING_WORK_NAME = "matutine-morning-run"

/**
 * Approximate morning insert. The period and the flex are both 24 hours, so the
 * platform may run this at any time. It never posts a notification.
 */
class MorningEditionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): ListenableWorker.Result {
        morningRepository(applicationContext).runMorning()
        return ListenableWorker.Result.success()
    }
}

internal data class MorningWorkSchedule(
    val uniqueName: String,
    val policy: ExistingPeriodicWorkPolicy,
    val request: androidx.work.PeriodicWorkRequest,
)

internal fun morningWorkSchedule(): MorningWorkSchedule {
    val request = PeriodicWorkRequestBuilder<MorningEditionWorker>(
        24, TimeUnit.HOURS,
        24, TimeUnit.HOURS,
    ).build()
    return MorningWorkSchedule(
        uniqueName = MORNING_WORK_NAME,
        policy = ExistingPeriodicWorkPolicy.KEEP,
        request = request,
    )
}

fun scheduleMorningRun(context: Context) {
    val schedule = morningWorkSchedule()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        schedule.uniqueName,
        schedule.policy,
        schedule.request,
    )
}

private fun morningRepository(context: Context) = LocalEditionRepository(
    FileEditionStore(androidDataRoot(context)),
    readFeed = ::readFeedDocument,
    writesDeviceCalendarSnapshot = writesDeviceCalendarSnapshot(),
    readDeviceCalendar = { day -> readDeviceCalendar(context, day) },
    readCalDav = ::readCalDavCalendar,
    summarize = ::summarizeWithGemini,
)
