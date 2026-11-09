package hu.toliver.matutine.store

import hu.toliver.matutine.edition.Edition
import hu.toliver.matutine.edition.EditionRole
import hu.toliver.matutine.edition.ReaderSettings
import hu.toliver.matutine.edition.SourceList
import hu.toliver.matutine.edition.decodeEdition
import hu.toliver.matutine.edition.decodeReaderSettings
import hu.toliver.matutine.edition.decodeSourceList
import hu.toliver.matutine.edition.encodeEdition
import hu.toliver.matutine.edition.encodeReaderSettings
import hu.toliver.matutine.edition.encodeSourceList
import java.io.File

actual class FileEditionStore actual constructor(
    private val rootPath: String,
) : hu.toliver.matutine.edition.EditionStore {
    private val root = File(rootPath)

    init {
        root.mkdirs()
    }

    override fun readSourceList(): SourceList? {
        val file = File(root, "sources.json")
        if (!file.exists()) return null
        return decodeSourceList(file.readText())
    }

    override fun writeSourceList(sourceList: SourceList) {
        File(root, "sources.json").writeText(encodeSourceList(sourceList))
    }

    override fun readSettings(): ReaderSettings? {
        val file = File(root, "settings.json")
        if (!file.exists()) return null
        return decodeReaderSettings(file.readText())
    }

    override fun writeSettings(settings: ReaderSettings) {
        File(root, "settings.json").writeText(encodeReaderSettings(settings))
    }

    override fun loadMorningEdition(dayKey: String): Edition? {
        val file = File(root, "editions/$dayKey-morning.json")
        if (!file.exists()) return null
        return decodeEdition(file.readText())
    }

    override fun saveMorningEdition(edition: Edition) {
        require(edition.role == EditionRole.morning)
        val dir = File(root, "editions")
        dir.mkdirs()
        val file = File(dir, "${edition.dayKey}-morning.json")
        file.writeText(encodeEdition(edition))
    }
}

fun desktopDataRoot(): String {
    val home = System.getProperty("user.home") ?: "."
    return File(home, ".local/share/Matutine").absolutePath
}
