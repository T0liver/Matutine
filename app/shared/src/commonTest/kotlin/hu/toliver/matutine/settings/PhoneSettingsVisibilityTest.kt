package hu.toliver.matutine.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhoneSettingsVisibilityTest {
    @Test
    fun configuredServerHidesSourcesAndForgetBringsThemBack() {
        val remote = phoneSettingsVisibility(serverConfigured = true)
        assertFalse(remote.showSources)
        assertFalse(remote.showGeminiKey)
        assertFalse(remote.showInterests)
        assertTrue(remote.offerForgetServer)

        val local = phoneSettingsVisibility(serverConfigured = false)
        assertTrue(local.showSources)
        assertTrue(local.showGeminiKey)
        assertTrue(local.showInterests)
        assertFalse(local.offerForgetServer)
    }
}
