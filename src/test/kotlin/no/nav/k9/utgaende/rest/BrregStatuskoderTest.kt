package no.nav.k9.utgaende.rest

import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import kotlin.test.assertEquals

internal class BrregStatuskoderTest {
    @Test
    fun `Kun Ok status filtrerer ikke borte noe`() {
        val filtrert = foretak.filtrerPåStatuskoder(brregResponseMedEnUnderstatus(
            hovedstatus = 0,
            understaus = 0
        ))
        assertEquals(foretak, filtrert)
    }

    @Test
    fun `Teknisk feil kaser feil`() {
        assertThrows(IllegalStateException::class.java) {
            foretak.filtrerPåStatuskoder(brregResponseMedEnUnderstatus(
                hovedstatus = -1,
                understaus = 777
            ))
        }
    }

    @Test
    fun `Inneholder en personstatus fjerner alle foretak`() {
        val filtrert = foretak.filtrerPåStatuskoder(brregResponseMedEnUnderstatus(
            hovedstatus = 1,
            understaus = 181
        ))
        assertEquals(emptySet(), filtrert)
    }

    @Test
    fun `Inneholder en uhåndter statuskombinasjon filtrerer ikke bort noe`(){
        val filtrert = foretak.filtrerPåStatuskoder(brregResponseMedEnUnderstatus(
            hovedstatus = 0,
            understaus = 1117
        ))
        assertEquals(foretak, filtrert)
    }

    @Test
    fun `Filtrer bort slettede enheter med status 1-120 og 1-100`() {
        val filtrert = foretak.filtrerPåStatuskoder(
            brregResponseMedFlerUnderstatuser(
                hovedstatus = 1,
                en = Pair(120, "Enhet 991 er slettet som dublett eller sammenslått - korrekt enhet 992 er slettet."),
                to = Pair(100, "Enhet 993 aldri opprettet.")
            )
        )
        assertEquals(emptySet(), filtrert)
    }

    @Test
    fun `Filtrer bort slettede enheter med status 0-1 og 0-2`() {
        val filtrert = foretak.filtrerPåStatuskoder(
            brregResponseMedFlerUnderstatuser(
                hovedstatus = 0,
                en = Pair(1, "Enhet 991 er slettet som dublett - korrekt enhet 993 er innført."),
                to = Pair(2, "Enhet 991 er slettet som sammenslått - korrekt enhet 993 er innført.")
            )
        )
        assertEquals(setOf(F_992, F_993), filtrert)
    }

    @Test
    fun `Ingen filtrering om slettinge gjelder enheter som ikke er i listen`() {
        val filtrert = foretak.filtrerPåStatuskoder(
            brregResponseMedFlerUnderstatuser(
                hovedstatus = 0,
                en = Pair(1, "Enhet 998 er slettet som dublett - korrekt enhet 993 er innført."),
                to = Pair(2, "Enhet 999 er slettet som sammenslått - korrekt enhet 993 er innført.")
            )
        )
        assertEquals(foretak, filtrert)
    }

    private companion object {
        internal val F_991 = Foretak(organisasjonsnummer = "991", registreringsdato = LocalDate.now(), rollebeskrivelse = "991")
        internal val F_992 = Foretak(organisasjonsnummer = "992", registreringsdato = LocalDate.now(), rollebeskrivelse = "992")
        internal val F_993 = Foretak(organisasjonsnummer = "993", registreringsdato = LocalDate.now(), rollebeskrivelse = "993")
        internal val foretak = mutableSetOf(F_991, F_992, F_993)

        internal fun brregResponseMedEnUnderstatus(
            hovedstatus:Int,
            understaus: Int) = JSONObject("""
        {
            "statuskoder": {
                "hovedstatus": $hovedstatus,
                "understatus": [{
                    "kode": $understaus,
                    "melding": "En melding her"
                }]
            }
        }
        """.trimIndent())

        internal fun brregResponseMedFlerUnderstatuser(
            hovedstatus: Int,
            en: Pair<Int, String>,
            to: Pair<Int, String>) = JSONObject("""
        {
            "statuskoder": {
                "hovedstatus": $hovedstatus,
                "understatus": [{
                    "kode": ${en.first},
                    "melding": "${en.second}"
                },
                {
                    "kode": ${to.first},
                    "melding": "${to.second}"
                }]
            }
        }
        """.trimIndent())
    }
}