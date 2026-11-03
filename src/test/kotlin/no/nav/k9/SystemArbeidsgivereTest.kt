package no.nav.k9

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.k9.PersonFødselsnummer.PERSON_1_MED_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_MED_FLERE_ARBEIDSFORHOLD_PER_ARBEIDSGIVER
import no.nav.k9.TokenUtils.hentToken
import no.nav.k9.integrasjon.common.NavHeaders
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.json.JsonCompareMode

class SystemArbeidsgivereTest : ApplicationTestBase() {

    private fun body(ident: String) = """
        {
            "ident": "$ident",
            "fraOgMed": "2014-01-01",
            "tilOgMed": "2030-12-31",
            "inkluderAlleAnsettelsesperioder": true
        }
    """.trimIndent()

    @Test
    fun `hent-arbeidsgivere med azure-token gir organisasjoner med alle ansettelsesperioder`() {
        //language=json
        val expectedResponse = """
            {
              "arbeidsgivere": {
                "organisasjoner": [
                  { "organisasjonsnummer": "123456789", "ansatt_fom": "2015-01-01", "ansatt_tom": "2019-12-31" },
                  { "organisasjonsnummer": "123456789", "ansatt_fom": "2020-01-01", "ansatt_tom": "2029-02-28" }
                ]
              }
            }
        """.trimIndent()

        client.post().uri("/system/hent-arbeidsgivere")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureToken()}")
            .header(X_CORRELATION_ID, "system-hent-arbeidsgivere")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(body(PERSON_MED_FLERE_ARBEIDSFORHOLD_PER_ARBEIDSGIVER))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `hent-arbeidsgivere kaller aareg med azure-token og identen fra body`() {
        wireMockServer.resetRequests()
        client.post().uri("/system/hent-arbeidsgivere")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureToken()}")
            .header(X_CORRELATION_ID, "system-aareg-token")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(body(PERSON_1_MED_BARN))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)

        val kall = wireMockServer.findAll(
            WireMock.getRequestedFor(WireMock.urlPathMatching("/arbeidsgiver-og-arbeidstaker-register-v2-mock/arbeidstaker/arbeidsforhold.*"))
        ).single()
        assertEquals(PERSON_1_MED_BARN, kall.getHeader(NavHeaders.PersonIdent))
        val claims = com.nimbusds.jwt.SignedJWT.parse(kall.getHeader(HttpHeaders.AUTHORIZATION).removePrefix("Bearer ")).jwtClaimsSet
        // Systemtoken fra Azure, ikke et vekslet borgertoken. Issuer-stien kommer fra mock-oauth2-server.
        assertEquals(true, claims.issuer.endsWith("/azure"))
    }

    @Test
    fun `hent-arbeidsgivere med tokenx-token gir 401`() {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.post().uri("/system/hent-arbeidsgivere")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "system-arbeidsgivere-tokenx")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(body(PERSON_1_MED_BARN))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `hent-arbeidsgivere med azure-token uten rolle gir 401`() {
        client.post().uri("/system/hent-arbeidsgivere")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureToken(claims = emptyMap())}")
            .header(X_CORRELATION_ID, "system-arbeidsgivere-uten-rolle")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(body(PERSON_1_MED_BARN))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `hent-arbeidsgivere gir 500 og ikke tom liste når aareg feiler`() {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathMatching("/arbeidsgiver-og-arbeidstaker-register-v2-mock/arbeidstaker/arbeidsforhold.*"))
                .willReturn(WireMock.serverError())
        )
        client.post().uri("/system/hent-arbeidsgivere")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureToken()}")
            .header(X_CORRELATION_ID, "system-arbeidsgivere-aareg-feil")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(body(PERSON_1_MED_BARN))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
