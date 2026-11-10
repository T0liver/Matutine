package hu.toliver.matutine.settings

import hu.toliver.matutine.edition.ServerLink

data class PhoneSettingsVisibility(
    val showSources: Boolean,
    val showGeminiKey: Boolean,
    val showInterests: Boolean,
    val offerForgetServer: Boolean,
)

fun phoneSettingsVisibility(serverConfigured: Boolean) = PhoneSettingsVisibility(
    showSources = !serverConfigured,
    showGeminiKey = !serverConfigured,
    showInterests = !serverConfigured,
    offerForgetServer = serverConfigured,
)

expect fun showsServerLinkSettings(): Boolean

expect fun readDeviceServerLink(dataRoot: String): ServerLink?

expect fun writeDeviceServerLink(dataRoot: String, link: ServerLink)

expect fun clearDeviceServerLink(dataRoot: String)
