package hu.toliver.matutine.store

import android.content.Context
import hu.toliver.matutine.edition.CalDavAccount
import hu.toliver.matutine.edition.CalendarSnapshot
import hu.toliver.matutine.edition.Edition
import hu.toliver.matutine.edition.EditionRole
import hu.toliver.matutine.edition.ReaderSettings
import hu.toliver.matutine.edition.ServerProcessConfig
import hu.toliver.matutine.edition.SourceList
import hu.toliver.matutine.edition.decodeServerProcessConfig
import hu.toliver.matutine.edition.encodeServerProcessConfig
import hu.toliver.matutine.edition.decodeCalDavAccount
import hu.toliver.matutine.edition.decodeCalendarSnapshot
import hu.toliver.matutine.edition.decodeEdition
import hu.toliver.matutine.edition.decodeReaderSettings
import hu.toliver.matutine.edition.decodeSourceList
import hu.toliver.matutine.edition.encodeCalDavAccount
import hu.toliver.matutine.edition.encodeCalendarSnapshot
import hu.toliver.matutine.edition.encodeEdition
import hu.toliver.matutine.edition.encodeReaderSettings
import hu.toliver.matutine.edition.encodeSourceList
import java.io.File

actual class FileEditionStore actual constructor(
    rootPath: String,
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

    override fun loadExtras(dayKey: String): List<Edition> {
        val dir = File(root, "editions")
        if (!dir.exists()) return emptyList()
        val prefix = "extra-$dayKey-"
        return dir.listFiles().orEmpty()
            .filter { it.isFile && it.name.startsWith(prefix) && it.name.endsWith(".json") }
            .map { decodeEdition(it.readText()) }
            .filter { it.role == EditionRole.extra && it.dayKey == dayKey }
            .sortedBy { it.id.substringAfterLast("-").toIntOrNull() ?: 0 }
    }

    override fun saveExtra(edition: Edition) {
        require(edition.role == EditionRole.extra)
        require(edition.id.startsWith("extra-${edition.dayKey}-"))
        val dir = File(root, "editions")
        dir.mkdirs()
        File(dir, "${edition.id}.json").writeText(encodeEdition(edition))
    }

    override fun readCalendarSnapshot(): CalendarSnapshot? {
        val file = File(root, "calendar-snapshot.json")
        if (!file.exists()) return null
        return decodeCalendarSnapshot(file.readText())
    }

    override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) {
        File(root, "calendar-snapshot.json").writeText(encodeCalendarSnapshot(snapshot))
    }

    override fun readCalDavAccount(): CalDavAccount? {
        val file = File(root, "caldav.json")
        if (!file.exists()) return null
        return decodeCalDavAccount(file.readText())
    }

    override fun writeCalDavAccount(account: CalDavAccount) {
        File(root, "caldav.json").writeText(encodeCalDavAccount(account))
    }

    override fun readProcessConfig(): ServerProcessConfig? {
        val file = File(root, "process.json")
        if (!file.exists()) return null
        return decodeServerProcessConfig(file.readText())
    }

    override fun writeProcessConfig(config: ServerProcessConfig) {
        File(root, "process.json").writeText(encodeServerProcessConfig(config))
    }
}

fun androidDataRoot(context: Context): String = context.filesDir.absolutePath
