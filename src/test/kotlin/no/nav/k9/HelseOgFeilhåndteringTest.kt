package no.nav.k9

import no.nav.k9.PersonFødselsnummer.PERSON_1_MED_BARN
import no.nav.k9.TokenUtils.hentToken
import no.nav.k9.integrasjon.common.NavHeaders
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

class HelseOgFeilhåndteringTest : ApplicationTestBase() {

    @Test
    fun `test readiness, liveness og metrics`() {
        client.get().uri("/health/readiness")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
        client.get().uri("/health/liveness")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
        client.get().uri("/metrics")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test oppslag uten idToken gir unauthorized`() {
        client.get().uri("/meg?a=aktør_id")
            .header(X_CORRELATION_ID, "meg-oppslag-uten-id-token")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test oppslag uten XCorrelationId gir BadRequest`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get().uri("/meg?a=aktør_id")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `ukjent path gir 404 og feil metode gir 405`() {
        client.get().uri("/finnes-ikke")
            .exchange()
            .expectStatus().isNotFound
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.post().uri("/meg")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "feil-metode-meg")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
    }

    @Test
    fun `meg uten X-K9-Ytelse gir 500`() {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get().uri("/meg?a=aktør_id")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "meg-uten-ytelse")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
    }

    @Test
    fun `meg med ugyldig X-K9-Ytelse gir 500`() {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get().uri("/meg?a=aktør_id")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "meg-ugyldig-ytelse")
            .header(NavHeaders.XK9Ytelse, "IKKE_EN_YTELSE")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
    }

    @Test
    fun `meg uten attributter og uten X-K9-Ytelse gir 500 fordi ytelse leses først`() {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get().uri("/meg")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "meg-uten-attributter-uten-ytelse")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `ugyldig format på X-Correlation-ID gir 400`() {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        listOf("abc", "har mellomrom", "ugyldig!tegn").forEach { correlationId ->
            client.get().uri("/meg?a=aktør_id")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                .header(X_CORRELATION_ID, correlationId)
                .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        }
    }

    @Test
    fun `meg uten token og uten correlation-id gir 400 fordi correlation-id sjekkes før autentisering`() {
        client.get().uri("/meg?a=aktør_id")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `system uten token og uten correlation-id gir 400 fordi correlation-id sjekkes før autentisering`() {
        client.post().uri("/system/hent-identer")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(hentIdenterBody)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
    }
}
