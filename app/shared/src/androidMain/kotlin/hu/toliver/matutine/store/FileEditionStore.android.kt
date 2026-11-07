package hu.toliver.matutine.store

import android.content.Context
import hu.toliver.matutine.edition.Edition
import hu.toliver.matutine.edition.EditionRole
import hu.toliver.matutine.edition.decodeEdition
import hu.toliver.matutine.edition.encodeEdition
import java.io.File

actual class FileEditionStore actual constructor(
    rootPath: String,
) : hu.toliver.matutine.edition.EditionStore {
    private val root = File(rootPath)

    init {
        root.mkdirs()
    }

    override fun hasSourceList(): Boolean = File(root, "sources.json").exists()

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

fun androidDataRoot(context: Context): String = context.filesDir.absolutePath
