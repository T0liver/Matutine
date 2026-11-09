package hu.toliver.matutine.store

import hu.toliver.matutine.edition.Edition
import hu.toliver.matutine.edition.ReaderSettings
import hu.toliver.matutine.edition.SourceList

actual class FileEditionStore actual constructor(
    @Suppress("UNUSED_PARAMETER") rootPath: String,
) : hu.toliver.matutine.edition.EditionStore {
    override fun readSourceList(): SourceList? = null

    override fun writeSourceList(sourceList: SourceList) = Unit

    override fun readSettings(): ReaderSettings? = null

    override fun writeSettings(settings: ReaderSettings) = Unit

    override fun loadMorningEdition(dayKey: String): Edition? = null

    override fun saveMorningEdition(edition: Edition) = Unit
}
