package hu.toliver.matutine

import kotlinx.browser.document
import org.w3c.dom.HTMLTextAreaElement

private const val TOKEN_ELEMENT_ID = "matutine-bootstrap-token"
private const val EDITION_ELEMENT_ID = "matutine-bootstrap-edition"

actual fun consumeMatutineBootstrap(): MatutineBootstrap? {
    val tokenNode = document.getElementById(TOKEN_ELEMENT_ID) ?: return null
    val token = tokenNode.textContent?.trim().orEmpty()
    if (token.isBlank()) return null
    tokenNode.textContent = ""
    return MatutineBootstrap(token = token)
}

actual fun consumeBootstrapEditionJson(): String? {
    val editionNode = document.getElementById(EDITION_ELEMENT_ID) as? HTMLTextAreaElement ?: return null
    val edition = editionNode.value.trim()
    if (edition.isBlank()) return null
    editionNode.value = ""
    return edition
}