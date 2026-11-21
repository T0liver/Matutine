package hu.toliver.matutine

import kotlinx.browser.window

actual fun readStoredBearerToken(): String? = window.sessionStorage.getItem(BEARER_SESSION_KEY)