package hu.toliver.matutine.edition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReaderSettingsTest {
    @Test
    fun emptySettingsDocument_defaultsNotificationTimeToSeven() {
        val settings = decodeReaderSettings("{}")
        assertEquals(DEFAULT_NOTIFICATION_TIME, settings.notificationTime)
        assertEquals(DEFAULT_NOTIFICATION_TIME, settings.resolvedNotificationTime())
    }

    @Test
    fun parseNotificationTime_acceptsHourAndMinute() {
        assertEquals(7 to 0, parseNotificationTime("07:00"))
        assertEquals(23 to 59, parseNotificationTime("23:59"))
        assertNull(parseNotificationTime("25:00"))
        assertNull(parseNotificationTime("seven"))
    }

    @Test
    fun resolvedNotificationTime_fallsBackWhenInvalid() {
        val settings = ReaderSettings(notificationTime = "bad")
        assertEquals(DEFAULT_NOTIFICATION_TIME, settings.resolvedNotificationTime())
    }

    @Test
    fun emptySettingsDocument_defaultsPaperCustomization() {
        val settings = decodeReaderSettings("{}")
        assertEquals(EDITION_READY_NOTIFICATION_BODY, settings.resolvedNotificationBody())
        assertEquals(DEFAULT_EDITION_THEME, settings.resolvedEditionTheme())
        assertEquals(DEFAULT_EDITION_LAYOUT, settings.resolvedEditionLayout())
    }

    @Test
    fun resolvedNotificationBody_fallsBackWhenBlank() {
        val settings = ReaderSettings(notificationBody = "  ")
        assertEquals(EDITION_READY_NOTIFICATION_BODY, settings.resolvedNotificationBody())
    }

    @Test
    fun resolvedThemeAndLayout_fallBackWhenUnknown() {
        val settings = ReaderSettings(editionTheme = "moonlit", editionLayout = "tabloid")
        assertEquals(DEFAULT_EDITION_THEME, settings.resolvedEditionTheme())
        assertEquals(DEFAULT_EDITION_LAYOUT, settings.resolvedEditionLayout())
    }
}
