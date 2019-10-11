import no.nav.k9.utgaende.rest.ForkortetNavn
import org.junit.jupiter.api.Test

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TpsProxyV1Test {

    @Test
    fun `Forkortet navn uten mellomnavn og under 25 tegn håndteres riktig`() {
        val forkortetNavn = ForkortetNavn("Forsman Erik")
        assertEquals("Erik", forkortetNavn.fornavn)
        assertEquals("Forsman", forkortetNavn.etternavn)
        assertTrue(forkortetNavn.erKomplett)
    }

    @Test
    fun `Forkortet navn med mellomnavn og under 25 tegn håndteres riktig`() {
        val forkortetNavn = ForkortetNavn("Forsman Erik Maximilian")
        assertEquals("Erik Maximilian", forkortetNavn.fornavn)
        assertEquals("Forsman", forkortetNavn.etternavn)
        assertTrue(forkortetNavn.erKomplett)
    }

    @Test
    fun `Forkortet navn med mellomnavn og over 25 tegn håndteres riktig`() {
        val forkortetNavn = ForkortetNavn("Forsman Erik Maximilian Bernadott".take(25))
        assertEquals("Erik Maximilian B", forkortetNavn.fornavn)
        assertEquals("Forsman", forkortetNavn.etternavn)
        assertFalse(forkortetNavn.erKomplett)
     }

    @Test
    fun `Forkortet navn uten mellomnavn og over 25 tegn håndteres riktig`() {
        val forkortetNavn = ForkortetNavn("Forsman ErikMaximilianBernadott".take(25))
        assertEquals("ErikMaximilianBer", forkortetNavn.fornavn)
        assertEquals("Forsman", forkortetNavn.etternavn)
        assertFalse(forkortetNavn.erKomplett)
    }

    @Test
    fun `Forkortet navn med etternavn på 25 tegn håndteres riktig`() {
        val forkortetNavn = ForkortetNavn("Et-veldig-langtetternavn ")
        assertEquals("", forkortetNavn.fornavn)
        assertEquals("Et-veldig-langtetternavn", forkortetNavn.etternavn)
        assertFalse(forkortetNavn.erKomplett)

    }

    @Test
    fun `Forkortet navn som er en tom String håndteres riktig`() {
        val forkortetNavn = ForkortetNavn("")
        assertEquals("", forkortetNavn.fornavn)
        assertEquals("", forkortetNavn.etternavn)
        assertFalse(forkortetNavn.erKomplett)
    }
}