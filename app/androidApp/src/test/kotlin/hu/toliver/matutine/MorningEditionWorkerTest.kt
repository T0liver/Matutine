package hu.toliver.matutine

import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class MorningEditionWorkerTest {
    @Test
    fun scheduleCoversAnyTimeInTheDayAndKeepsTheExistingRequest() {
        val schedule = morningWorkSchedule()

        assertEquals("matutine-morning-run", schedule.uniqueName)
        assertEquals(androidx.work.ExistingPeriodicWorkPolicy.KEEP, schedule.policy)
        assertEquals(TimeUnit.HOURS.toMillis(24), schedule.request.workSpec.intervalDuration)
        assertEquals(schedule.request.workSpec.intervalDuration, schedule.request.workSpec.flexDuration)
        assertEquals(
            MorningEditionWorker::class.java.name,
            schedule.request.workSpec.workerClassName,
        )
    }

    @Test
    fun remoteMorningTickDoesNotInsert() {
        assertEquals(false, morningTickInserts(serverConfigured = true))
        assertEquals(true, morningTickInserts(serverConfigured = false))
    }
}
