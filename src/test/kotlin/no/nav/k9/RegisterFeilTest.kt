package no.nav.k9

import com.github.tomakehurst.wiremock.client.WireMock
import no.nav.k9.PersonFødselsnummer.PERSON_1_MED_BARN
import no.nav.k9.TokenUtils.hentToken
import no.nav.k9.integrasjon.common.NavHeaders
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.servlet.client.expectBody

class RegisterFeilTest : ApplicationTestBase() {

    @Test
    fun `aareg som svarer 500 gir 500 etter retry`() {
        val fnr = "01010010098"
        val stub = wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathMatching("/arbeidsgiver-og-arbeidstaker-register-v2-mock/arbeidstaker/arbeidsforhold.*"))
                .withHeader(NavHeaders.PersonIdent, WireMock.equalTo(fnr))
                .atPriority(1)
                .willReturn(WireMock.aResponse().withStatus(500).withBody("""{"feil":"aareg nede"}"""))
        )
        try {
            val idToken = mockOAuth2Server.hentToken(subject = fnr)
            client.get().uri("/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                .header(X_CORRELATION_ID, "aareg-500")
                .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody<String>().value { body ->
                    assertFalse(body.orEmpty().contains("aareg nede"), "Feilmeldingen fra aareg skal ikke lekke ut")
                }
            wireMockServer.verify(
                3,
                WireMock.getRequestedFor(WireMock.urlPathMatching("/arbeidsgiver-og-arbeidstaker-register-v2-mock/arbeidstaker/arbeidsforhold.*"))
                    .withHeader(NavHeaders.PersonIdent, WireMock.equalTo(fnr))
            )
        } finally {
            wireMockServer.removeStub(stub)
        }
    }

    @Test
    fun `aareg som svarer 4xx retryes ikke`() {
        val fnr = "01010010099"
        val urlPattern = WireMock.urlPathMatching("/arbeidsgiver-og-arbeidstaker-register-v2-mock/arbeidstaker/arbeidsforhold.*")
        val stub = wireMockServer.stubFor(
            WireMock.get(urlPattern)
                .withHeader(NavHeaders.PersonIdent, WireMock.equalTo(fnr))
                .atPriority(1)
                .willReturn(WireMock.aResponse().withStatus(403))
        )
        try {
            val idToken = mockOAuth2Server.hentToken(subject = fnr)
            client.get().uri("/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                .header(X_CORRELATION_ID, "aareg-403")
                .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            wireMockServer.verify(
                1,
                WireMock.getRequestedFor(urlPattern).withHeader(NavHeaders.PersonIdent, WireMock.equalTo(fnr))
            )
        } finally {
            wireMockServer.removeStub(stub)
        }
    }

    @Test
    fun `ereg som svarer 500 gir organisasjon uten navn`() {
        val stub = wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathMatching("/enhets-register-mock/organisasjon/123456789/noekkelinfo"))
                .atPriority(1)
                .willReturn(WireMock.aResponse().withStatus(500))
        )
        try {
            val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
            client.get().uri("/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                .header(X_CORRELATION_ID, "ereg-500")
                .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.OK)
                .expectBody().json("""{ "arbeidsgivere": { "organisasjoner": [ { "organisasjonsnummer": "123456789" } ] } }""", JsonCompareMode.STRICT)
        } finally {
            wireMockServer.removeStub(stub)
        }
    }

    @Test
    fun `PDL som returnerer errors gir 500`() {
        val fnr = "01010010097"
        val stub = wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathMatching("/graphql"))
                .withRequestBody(WireMock.matchingJsonPath("$.variables.ident", WireMock.equalTo(fnr)))
                .atPriority(1)
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""{ "errors": [ { "message": "pdl feil" } ], "data": null }""")
                )
        )
        try {
            val idToken = mockOAuth2Server.hentToken(subject = fnr)
            client.get().uri("/meg?a=aktør_id")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                .header(X_CORRELATION_ID, "pdl-errors")
                .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
                .expectBody<String>().value { body ->
                    assertFalse(body.orEmpty().contains("pdl feil"), "Feilmeldingen fra PDL skal ikke lekke ut")
                }
        } finally {
            wireMockServer.removeStub(stub)
        }
    }
}
