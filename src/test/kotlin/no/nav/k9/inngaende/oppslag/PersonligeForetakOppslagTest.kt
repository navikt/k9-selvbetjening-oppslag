package no.nav.k9.inngaende.oppslag

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import no.nav.k9.utgaende.gateway.BrregProxyV1Gateway
import no.nav.k9.utgaende.gateway.EnhetsregisterV1Gateway
import no.nav.k9.utgaende.rest.Enhet
import no.nav.k9.utgaende.rest.Foretak
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PersonligeForetakOppslagTest {

    @MockK
    lateinit var enhetsregisterV1Gateway : EnhetsregisterV1Gateway

    @MockK
    lateinit var brregProxyV1Gateway : BrregProxyV1Gateway

    lateinit var personligeForetakOppslag: PersonligeForetakOppslag

    @BeforeAll
    internal fun beforeAll() {
        personligeForetakOppslag = PersonligeForetakOppslag(
            enhetsregisterV1Gateway = enhetsregisterV1Gateway,
            brregProxyV1Gateway = brregProxyV1Gateway
        )
    }

    @Test
    fun `Innehaver av et enkeltpersonforetak`() {
        mockBrreg(setOf(foretak("Innehaver", "123")))
        mockEreg(enhet("ENK"))
        val personligeForetak = hentPersonligeForetak()
        assertNotNull(personligeForetak)
        assertEquals(1, personligeForetak!!.size)
        assertEquals(personligeForetak.first().organisasjonsummer, "123")
        assertEquals(personligeForetak.first().organisasjonsform, "ENK")
    }

    @Test
    fun `Daglig leder av et enkeltpersonforetak`() {
        mockBrreg(setOf(foretak("Daglig leder/ adm.direktør", "123")))
        mockEreg(enhet("ENK"))
        val personligeForetak = hentPersonligeForetak()
        assertNotNull(personligeForetak)
        assertTrue(personligeForetak!!.isEmpty())
    }

    @Test
    fun `Fler roller i samme ansvarlige foretak`() {
        mockBrreg(setOf(
            foretak("Deltaker med delt ansvar", "123"),
            foretak("Deltaker med fullt ansvar", "123")
        ))
        mockEreg(enhet("ANS"))
        val personligeForetak = hentPersonligeForetak()
        assertNotNull(personligeForetak)
        assertEquals(1, personligeForetak!!.size)
        assertEquals(personligeForetak.first().organisasjonsummer, "123")
        assertEquals(personligeForetak.first().organisasjonsform, "ANS")
    }

    @Test
    fun `Enkeltpersonforetak - ansvarslig fortak og delt ansvar`() {
        mockBrreg(setOf(
            foretak("Deltaker med delt ansvar", "123"),
            foretak("Deltaker med fullt ansvar", "234"),
            foretak("Innehaver", "567")
        ))
        mockEreg(setOf(
            "123" to enhet("da"),
            "234" to enhet("ans"),
            "567" to enhet("enk")
        ))

        val personligeForetak = hentPersonligeForetak()
        assertNotNull(personligeForetak)
        assertEquals(3, personligeForetak!!.size)
        assertTrue(personligeForetak.any { it.organisasjonsform == "DA" && it.organisasjonsummer == "123" })
        assertTrue(personligeForetak.any { it.organisasjonsform == "ANS" && it.organisasjonsummer == "234" })
        assertTrue(personligeForetak.any { it.organisasjonsform == "ENK" && it.organisasjonsummer == "567" })
    }

    @Test
    fun `Kun roller i foretak som ikke er personlige`() {
        mockBrreg(setOf(
            foretak("Daglig leder/administrerende direktør", "123"),
            foretak("Styrets leder", "456")
        ))
        mockEreg(enhet("AS"))
        val personligeForetak = hentPersonligeForetak()
        assertNotNull(personligeForetak)
        assertTrue(personligeForetak!!.isEmpty())
    }

    private fun mockBrreg(foretak: Set<Foretak>) {
        every { runBlocking { brregProxyV1Gateway.foretak(any(), any())} } returns foretak
    }

    private fun mockEreg(enhet: Enhet) {
        every { runBlocking { enhetsregisterV1Gateway.enhet(any(), any())} } returns enhet
    }

    private fun mockEreg(kombinasjoner: Set<Pair<String, Enhet>>) {
        clearMocks(enhetsregisterV1Gateway)
        kombinasjoner.forEach {
            every { runBlocking { enhetsregisterV1Gateway.enhet(it.first, any())} } returns it.second
        }

    }

    private fun hentPersonligeForetak() = runBlocking {
        personligeForetakOppslag.personligeForetak(Ident("1"), setOf(Attributt.personligForetakOrganisasjonsnummer))
    }

    private companion object {
        private fun foretak(rollebeskrivelse: String, organisasjonsnummer: String) = Foretak(
            organisasjonsnummer = organisasjonsnummer,
            registreringsdato = LocalDate.now(),
            rollebeskrivelser = setOf(rollebeskrivelse)
        )
        private fun enhet(enhetstype: String) = Enhet(
            organisasjonsnummer = UUID.randomUUID().toString(),
            navn = "Foo",
            enhetstype = enhetstype,
            opphørsdato = null
        )
    }
}