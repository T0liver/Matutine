package hu.toliver.matutine.edition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebPaperGateTest {

    @Test
    fun wrongToken_keepsTheTypedValueAndDoesNotOpenThePaper() {
        val decision = webPaperGate(typedToken = "not-the-token", httpStatus = 401)
        assertEquals("not-the-token", decision.typedToken)
        assertEquals(TOKEN_DOES_NOT_MATCH, decision.message)
        assertFalse(decision.showEdition)
    }

    @Test
    fun rightToken_opensTheEditionWithoutAGateMessage() {
        val decision = webPaperGate(typedToken = "matutine-demo", httpStatus = 200)
        assertEquals("matutine-demo", decision.typedToken)
        assertNull(decision.message)
        assertTrue(decision.showEdition)
    }

    @Test
    fun otherFailure_staysOnTheGateWithoutAPaper() {
        val decision = webPaperGate(typedToken = "matutine-demo", httpStatus = 503)
        assertEquals("matutine-demo", decision.typedToken)
        assertNull(decision.message)
        assertFalse(decision.showEdition)
    }

    @Test
    fun sampleEditionCarriesTheSampleLine() {
        val edition = SampleGenerator.forDay(todayLocalDate())
        val decoded = decodeEdition(encodeEdition(edition))
        assertTrue(decoded.isSample)
        assertTrue(EditionTranscript.render(decoded).contains(EditionTranscript.SAMPLE))
        assertEquals("SAMPLE EDITION", EditionTranscript.SAMPLE)
    }
}
