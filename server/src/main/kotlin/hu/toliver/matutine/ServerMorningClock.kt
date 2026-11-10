package hu.toliver.matutine

import hu.toliver.matutine.edition.DEFAULT_NOTIFICATION_TIME
import hu.toliver.matutine.edition.EditionStore
import hu.toliver.matutine.edition.LocalEditionRepository
import hu.toliver.matutine.edition.parseNotificationTime
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.seconds

internal fun resolveServerTimeZone(raw: String?): TimeZone {
    val zone = raw?.takeIf { it.isNotBlank() } ?: return TimeZone.currentSystemDefault()
    return TimeZone.of(zone)
}

/** Blank is 07:00. A string [parseNotificationTime] rejects does not arm the clock. */
internal fun resolveGenerationTime(raw: String?): Pair<Int, Int>? {
    if (raw.isNullOrBlank()) return parseNotificationTime(DEFAULT_NOTIFICATION_TIME)
    return parseNotificationTime(raw)
}

internal data class MorningClockMark(val attempted: LocalDate? = null)

/**
 * One insert the first time this process observes the generation minute.
 * A later minute the same day is not a catch-up.
 */
internal fun morningClockAdvance(
    mark: MorningClockMark,
    now: Instant,
    zone: TimeZone,
    hour: Int,
    minute: Int,
): Pair<MorningClockMark, Boolean> {
    val local = now.toLocalDateTime(zone)
    val due = local.hour == hour && local.minute == minute
    if (!due || mark.attempted == local.date) return mark to false
    return mark.copy(attempted = local.date) to true
}

internal suspend fun insertScheduledMorning(
    store: EditionStore,
    repository: LocalEditionRepository,
) {
    if (store.readSourceList() == null) {
        repository.loadCurrentMorningEdition().getOrThrow()
    } else {
        repository.runMorning().getOrThrow()
    }
}

internal class ServerMorningClock(
    private val zone: TimeZone,
    private val hour: Int,
    private val minute: Int,
    private val clock: () -> Instant,
    private val insert: suspend () -> Unit,
) {
    private var mark = MorningClockMark()

    suspend fun tick() {
        val (next, due) = morningClockAdvance(mark, clock(), zone, hour, minute)
        mark = next
        if (!due) return
        try {
            insert()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            // A failed insert leaves the store unchanged and the process stays up.
        }
    }

    suspend fun run() {
        while (true) {
            tick()
            delay(1.seconds)
        }
    }
}
