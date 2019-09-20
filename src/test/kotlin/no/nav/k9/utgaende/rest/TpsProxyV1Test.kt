import no.nav.k9.utgaende.rest.ForkortetNavn
import org.junit.jupiter.api.Test

import kotlin.test.assertEquals
import kotlin.test.assertNull

class TpsProxyV1Test  {

    @Test
    fun `Forkortet navn uten mellomnavn og under 25 tegn håndteres riktig`() {
        val forkortetNavn = ForkortetNavn("Forsman Erik")
        assertEquals("Erik", forkortetNavn.fornavn)
        assertEquals("Forsman", forkortetNavn.etternavn)
        assertNull(forkortetNavn.mellomnavn)
    }

    @Test
    fun `Forkortet navn med mellomnavn og under 25 tegn håndteres riktig`() {
        val forkortetNavn = ForkortetNavn("Forsman Erik Maximilian")
        assertEquals("Erik", forkortetNavn.fornavn)
        assertEquals("Forsman", forkortetNavn.etternavn)
        assertEquals("Maximilian", forkortetNavn.mellomnavn)
    }

    @Test
    fun `Forkortet navn med mellomnavn og over 25 tegn håndteres riktig`() {
        val forkortetNavn = ForkortetNavn("Forsman Erik Maximilian Bernadott".take(25))
        assertEquals("Erik", forkortetNavn.fornavn)
        assertEquals("Forsman", forkortetNavn.etternavn)
        assertEquals("Maximilian B", forkortetNavn.mellomnavn)
    }

    @Test
    fun `Forkortet navn uten mellomnavn og over 25 tegn håndteres riktig`() {
        val forkortetNavn = ForkortetNavn("Forsman ErikMaximilianBernadott".take(25))
        assertEquals("ErikMaximilianBer", forkortetNavn.fornavn)
        assertEquals("Forsman", forkortetNavn.etternavn)
        assertNull(forkortetNavn.mellomnavn)
    }

    @Test
    fun `Forkortet navn med etternavn på 25 tegn håndteres riktig`() {
        val forkortetNavn = ForkortetNavn("Et-veldig-langtetternavn ")
        assertEquals("", forkortetNavn.fornavn)
        assertEquals("Et-veldig-langtetternavn", forkortetNavn.etternavn)
        assertNull(forkortetNavn.mellomnavn)
    }
}