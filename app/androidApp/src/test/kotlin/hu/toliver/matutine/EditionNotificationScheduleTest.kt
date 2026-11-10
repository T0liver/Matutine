package hu.toliver.matutine

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditionNotificationScheduleTest {
    @Test
    fun delayTargetsTheNextOccurrenceToday() {
        val zone = ZoneId.of("UTC")
        val now = ZonedDateTime.of(LocalDateTime.of(2026, 11, 10, 6, 30), zone)
        val delay = millisUntilNextNotification(7, 0, now)
        assertEquals(TimeUnit.MINUTES.toMillis(30), delay)
    }

    @Test
    fun delayRollsToTomorrowAfterTheTimePasses() {
        val zone = ZoneId.of("UTC")
        val now = ZonedDateTime.of(LocalDateTime.of(2026, 11, 10, 7, 1), zone)
        val delay = millisUntilNextNotification(7, 0, now)
        assertEquals(TimeUnit.HOURS.toMillis(23) + TimeUnit.MINUTES.toMillis(59), delay)
    }

    @Test
    fun notificationWorkUsesReplacePolicy() {
        val schedule = editionNotificationWorkSchedule(7, 0, ZonedDateTime.of(LocalDateTime.of(2026, 11, 10, 6, 0), ZoneId.of("UTC")))
        assertEquals(EDITION_NOTIFICATION_WORK_NAME, schedule.uniqueName)
        assertEquals(androidx.work.ExistingWorkPolicy.REPLACE, schedule.policy)
        assertEquals(
            EditionNotificationWorker::class.java.name,
            schedule.request.workSpec.workerClassName,
        )
    }

    @Test
    fun missingOrUnreachableMorningDoesNotPostANotification() {
        assertFalse(morningEditionWasReturned(null))
        assertFalse(morningEditionWasReturned(404))
        assertFalse(morningEditionWasReturned(401))
        assertTrue(morningEditionWasReturned(200))
        assertFalse(shouldPostEditionNotification(notificationsAllowed = true, morningReturned = false))
        assertFalse(shouldPostEditionNotification(notificationsAllowed = false, morningReturned = true))
        assertTrue(shouldPostEditionNotification(notificationsAllowed = true, morningReturned = true))
    }
}
