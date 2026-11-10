package hu.toliver.matutine

import hu.toliver.matutine.edition.showsTypewriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedLogicDesktopTest {

    @Test
    fun example() {
        assertEquals(3, 1 + 2)
    }

    @Test
    fun desktopHasTheTypewriter() {
        assertTrue(showsTypewriter())
    }
}