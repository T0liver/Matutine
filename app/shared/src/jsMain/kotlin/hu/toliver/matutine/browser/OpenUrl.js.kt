package hu.toliver.matutine.browser

actual fun openUrl(url: String) {
    kotlinx.browser.window.open(url, "_blank")
}
