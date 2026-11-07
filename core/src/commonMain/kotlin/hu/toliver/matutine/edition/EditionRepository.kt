package hu.toliver.matutine.edition

import kotlinx.datetime.LocalDate

interface EditionStore {
    fun hasSourceList(): Boolean
    fun loadMorningEdition(dayKey: String): Edition?
    fun saveMorningEdition(edition: Edition)
}

class LocalEditionRepository(
    private val store: EditionStore,
    private val today: () -> LocalDate = { todayLocalDate() },
    private val forceSampleLoadFailure: () -> Boolean = { false },
) : EditionRepository {
    override suspend fun loadCurrentMorningEdition(): Result<Edition> = runCatching {
        val dayKey = today().toDayKey()
        store.loadMorningEdition(dayKey)?.let { return@runCatching it }

        if (store.hasSourceList()) {
            error("Today's morning edition is not available yet.")
        }

        if (forceSampleLoadFailure()) {
            error("sample_load_failed")
        }

        SampleEdition.forDay(today()).also { sample ->
            store.saveMorningEdition(sample)
        }
    }

    override suspend fun retrySample(): Result<Edition> = runCatching {
        if (store.hasSourceList()) {
            error("Sample is not used when a source list exists.")
        }
        if (forceSampleLoadFailure()) {
            error("sample_load_failed")
        }
        SampleEdition.forDay(today()).also { store.saveMorningEdition(it) }
    }
}

interface EditionRepository {
    suspend fun loadCurrentMorningEdition(): Result<Edition>
    suspend fun retrySample(): Result<Edition>
}
