package hu.toliver.matutine.edition

const val TOKEN_DOES_NOT_MATCH = "That token doesn't match."

data class WebPaperGate(
    val typedToken: String,
    val showEdition: Boolean,
    val message: String?,
)

/**
 * The browser gate does not compare the token itself. A 200 from the current-edition
 * route is the only path that opens the paper. The typed value is returned unchanged.
 */
fun webPaperGate(typedToken: String, httpStatus: Int): WebPaperGate = when (httpStatus) {
    200 -> WebPaperGate(typedToken = typedToken, showEdition = true, message = null)
    401 -> WebPaperGate(
        typedToken = typedToken,
        showEdition = false,
        message = TOKEN_DOES_NOT_MATCH,
    )
    else -> WebPaperGate(typedToken = typedToken, showEdition = false, message = null)
}
