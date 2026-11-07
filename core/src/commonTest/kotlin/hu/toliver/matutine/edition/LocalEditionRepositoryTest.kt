package hu.toliver.matutine.edition

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeStore(
    private var morning: Edition? = null,
    private var sourceList: Boolean = false,
) : EditionStore {
    override fun hasSourceList(): Boolean = sourceList

    override fun loadMorningEdition(dayKey: String): Edition? =
        morning?.takeIf { it.dayKey == dayKey && it.role == EditionRole.morning }

    override fun saveMorningEdition(edition: Edition) {
        morning = edition
    }
}

class LocalEditionRepositoryTest {
    private val sampleDay = LocalDate(2026, 11, 7)

    @Test
    fun firstLaunchWithoutSourceList_servesSample() = runBlocking {
        val repo = LocalEditionRepository(FakeStore(), today = { sampleDay })
        val edition = repo.loadCurrentMorningEdition().getOrThrow()
        assertTrue(edition.isSample)
        assertEquals("Matutine", edition.mastheadName)
        assertEquals("2026-11-07", edition.dayKey)
        assertTrue(edition.stories.any { it.rank == StoryRank.lead })
        assertTrue(edition.stories.any { it.rank == StoryRank.secondary })
    }

    @Test
    fun savedMorningEdition_preferredOverSample() = runBlocking {
        val saved = SampleEdition.forDay(sampleDay).copy(id = "saved", isSample = false)
        val repo = LocalEditionRepository(FakeStore(morning = saved), today = { sampleDay })
        val edition = repo.loadCurrentMorningEdition().getOrThrow()
        assertEquals("saved", edition.id)
        assertFalse(edition.isSample)
    }
}
