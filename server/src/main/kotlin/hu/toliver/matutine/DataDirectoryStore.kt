package hu.toliver.matutine

import hu.toliver.matutine.edition.CalDavAccount
import hu.toliver.matutine.edition.CalendarSnapshot
import hu.toliver.matutine.edition.Edition
import hu.toliver.matutine.edition.EditionRole
import hu.toliver.matutine.edition.EditionStore
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

/**
 * Same document names as the desktop file store, rooted at the server data directory.
 * The server does not depend on `:app:shared` because that module pulls Compose.
 */
class DataDirectoryStore(
    rootPath: String,
) : EditionStore {
    private val root = File(rootPath)

    init {
        root.mkdirs()
    }

    override fun readSourceList(): SourceList? = read("sources.json", ::decodeSourceList)

    override fun writeSourceList(sourceList: SourceList) {
        write("sources.json", encodeSourceList(sourceList))
    }

    override fun readSettings(): ReaderSettings? = read("settings.json", ::decodeReaderSettings)

    override fun writeSettings(settings: ReaderSettings) {
        write("settings.json", encodeReaderSettings(settings))
    }

    override fun loadMorningEdition(dayKey: String): Edition? =
        read("editions/$dayKey-morning.json", ::decodeEdition)

    override fun saveMorningEdition(edition: Edition) {
        require(edition.role == EditionRole.morning)
        write("editions/${edition.dayKey}-morning.json", encodeEdition(edition))
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
        write("editions/${edition.id}.json", encodeEdition(edition))
    }

    override fun readCalendarSnapshot(): CalendarSnapshot? =
        read("calendar-snapshot.json", ::decodeCalendarSnapshot)

    override fun writeCalendarSnapshot(snapshot: CalendarSnapshot) {
        write("calendar-snapshot.json", encodeCalendarSnapshot(snapshot))
    }

    override fun readCalDavAccount(): CalDavAccount? = read("caldav.json", ::decodeCalDavAccount)

    override fun writeCalDavAccount(account: CalDavAccount) {
        write("caldav.json", encodeCalDavAccount(account))
    }

    override fun readProcessConfig(): ServerProcessConfig? =
        read("process.json", ::decodeServerProcessConfig)

    override fun writeProcessConfig(config: ServerProcessConfig) {
        write("process.json", encodeServerProcessConfig(config))
    }

    private fun <T> read(relativePath: String, decode: (String) -> T): T? {
        val file = File(root, relativePath)
        if (!file.exists()) return null
        return decode(file.readText())
    }

    private fun write(relativePath: String, json: String) {
        val file = File(root, relativePath)
        file.parentFile?.mkdirs()
        file.writeText(json)
    }
}
