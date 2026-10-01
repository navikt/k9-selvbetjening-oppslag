package no.nav.k9

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.k9.PersonFødselsnummer.PERSON_1_MED_BARN
import no.nav.k9.TokenUtils.hentToken
import no.nav.k9.integrasjon.common.NavHeaders
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.time.LocalDate.parse

class UtgåendeKallTest : ApplicationTestBase() {

    @Test
    fun `utgående kall til PDL bærer Nav-Call-Id fra X-Correlation-ID`() {
        wireMockServer.resetRequests()
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get().uri("/meg?a=aktør_id")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "call-id-propagering")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
        wireMockServer.verify(
            WireMock.postRequestedFor(WireMock.urlPathMatching("/graphql"))
                .withHeader(NavHeaders.CallId, WireMock.equalTo("call-id-propagering"))
                .withHeader(NavHeaders.Tema, WireMock.equalTo("OMS"))
        )
    }

    @Test
    fun `token-exchange mot PDL bruker subject-token fra requesten`() {
        wireMockServer.resetRequests()
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get().uri("/meg?a=aktør_id")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "subject-token-pdl")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)

        val autorisering = wireMockServer.findAll(WireMock.postRequestedFor(WireMock.urlPathMatching("/graphql")))
            .map { it.getHeader(HttpHeaders.AUTHORIZATION) }
        assertTrue(autorisering.isNotEmpty())
        autorisering.forEach { header ->
            val claims = com.nimbusds.jwt.SignedJWT.parse(header.removePrefix("Bearer ")).jwtClaimsSet
            assertEquals(PERSON_1_MED_BARN, claims.subject)
            assertEquals(listOf("dev-fss:pdl:pdl-api"), claims.audience)
        }
    }

    @Test
    fun `utgående kall til aareg bærer Nav-Call-Id og token-exchange-token`() {
        wireMockServer.resetRequests()
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get().uri("/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "call-id-aareg")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)

        val kall = wireMockServer.findAll(
            WireMock.getRequestedFor(WireMock.urlPathMatching("/arbeidsgiver-og-arbeidstaker-register-v2-mock/arbeidstaker/arbeidsforhold.*"))
        )
        assertEquals(1, kall.size)
        assertEquals("call-id-aareg", kall.single().getHeader(NavHeaders.CallId))
        assertEquals(PERSON_1_MED_BARN, kall.single().getHeader(NavHeaders.PersonIdent))
        val claims = com.nimbusds.jwt.SignedJWT.parse(kall.single().getHeader(HttpHeaders.AUTHORIZATION).removePrefix("Bearer ")).jwtClaimsSet
        assertEquals(listOf("dev-fss.arbeidsforhold.aareg-services-nais"), claims.audience)
    }

    @Test
    fun `utgående kall til aareg har query-parametre for type og status`() {
        wireMockServer.resetRequests()
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get().uri("/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "query-aareg")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)

        val kall = wireMockServer.findAll(
            WireMock.getRequestedFor(WireMock.urlPathMatching("/arbeidsgiver-og-arbeidstaker-register-v2-mock/arbeidstaker/arbeidsforhold.*"))
        ).single()
        assertEquals(
            "ordinaertArbeidsforhold,maritimtArbeidsforhold,forenkletOppgjoersordning,frilanserOppdragstakerHonorarPersonerMm",
            kall.queryParameter("arbeidsforholdtype").firstValue()
        )
        assertEquals("AKTIV,AVSLUTTET,FREMTIDIG", kall.queryParameter("arbeidsforholdstatus").firstValue())
    }
}
