package hu.toliver.matutine.store

import hu.toliver.matutine.edition.EditionStore

expect class FileEditionStore : EditionStore {
    constructor(rootPath: String)
}
