package hu.toliver.matutine.store

import hu.toliver.matutine.edition.Edition

actual class FileEditionStore actual constructor(
    @Suppress("UNUSED_PARAMETER") rootPath: String,
) : hu.toliver.matutine.edition.EditionStore {
    override fun hasSourceList(): Boolean = false

    override fun loadMorningEdition(dayKey: String): Edition? = null

    override fun saveMorningEdition(edition: Edition) = Unit
}
