package hu.toliver.matutine

import hu.toliver.matutine.edition.showsTypewriter
import kotlin.test.Test
import kotlin.test.assertFalse

class TypewriterPresenceTest {
    @Test
    fun androidHasNoTypewriter() {
        assertFalse(showsTypewriter())
    }
}
