package no.nav.k9

import no.nav.k9.integrasjon.aareg.erAnsattIPerioden
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate.parse

class ErAnsattIPeriodenTest {

    @Test
    fun `Test av erAnsattIPerioden`() {
        val mandag = parse("2022-01-07")
        val tirsdag = mandag.plusDays(1)
        val onsdag = tirsdag.plusDays(1)
        val torsdag = onsdag.plusDays(1)
        val fredag = torsdag.plusDays(1)

        val ansattFOM = tirsdag
        val ansattTOM = torsdag

        assertTrue(erAnsattIPerioden(ansattFOM, ansattTOM, tirsdag, torsdag)) //Samme dato
        assertTrue(erAnsattIPerioden(ansattFOM, ansattTOM, tirsdag, fredag)) //En dag ekstra TOM
        assertTrue(erAnsattIPerioden(ansattFOM, ansattTOM, mandag, torsdag)) //En dag ekstra FOM
        assertTrue(erAnsattIPerioden(ansattFOM, ansattTOM, onsdag, fredag)) //Overlapp midt i
        assertTrue(erAnsattIPerioden(ansattFOM, ansattTOM, mandag, fredag)) //Full overlapp

        assertFalse(erAnsattIPerioden(ansattFOM, ansattTOM, mandag, mandag)) //En dag før
        assertFalse(erAnsattIPerioden(ansattFOM, ansattTOM, fredag, fredag)) //En dag etter

        assertTrue(erAnsattIPerioden(ansattFOM, null, fredag, fredag)) //Har ingen TOM
    }
}
