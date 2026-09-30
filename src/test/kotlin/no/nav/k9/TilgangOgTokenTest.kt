package no.nav.k9

import no.nav.k9.PersonFødselsnummer.PERSON_1_MED_BARN
import no.nav.k9.TokenUtils.hentToken
import no.nav.k9.integrasjon.common.NavHeaders
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.util.*

class TilgangOgTokenTest : ApplicationTestBase() {

    @Test
    fun `test megOppslag med acr idporten-loa-high`() {
        val idToken: String = mockOAuth2Server.hentToken(
            subject = PERSON_1_MED_BARN,
            claims = mapOf("acr" to "idporten-loa-high")
        )
        client.get().uri("/meg?a=aktør_id")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "meg-oppslag-idporten-loa-high")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test megOppslag med utilstrekkelig acr gir unauthorized`() {
        val idToken: String = mockOAuth2Server.hentToken(
            subject = PERSON_1_MED_BARN,
            claims = mapOf("acr" to "Level3")
        )
        client.get().uri("/meg?a=aktør_id")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "meg-oppslag-for-lav-acr")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `megOppslag med azure token skal gi 401 feil`() {

        val azureToken = mockOAuth2Server.issueToken(
            issuerId = "azure",
            subject = UUID.randomUUID().toString(),
            audience = "dev-fss:dusseldorf:k9-selvbetjening-oppslag",
            claims = mapOf("role" to "access_as_application")
        ).serialize()

        client.get().uri("/meg?a=aktør_id")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $azureToken")
            .header(X_CORRELATION_ID, "meg-oppslag-aktoer-id")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `meg med tokenx-token uten acr Level4 gir 401`() {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN, claims = mapOf("acr" to "Level3"))
        client.get().uri("/meg?a=aktør_id")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "meg-uten-level4")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
        val utenAcr = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN, claims = emptyMap())
        client.get().uri("/meg?a=aktør_id")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $utenAcr")
            .header(X_CORRELATION_ID, "meg-uten-acr")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `arbeidsgivere med azure-token gir 401`() {
        client.get().uri("/arbeidsgivere?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureToken()}")
            .header(X_CORRELATION_ID, "arbeidsgivere-azure")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `system med tokenx-token gir 401`() {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.post().uri("/system/hent-identer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "system-med-tokenx")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(hentIdenterBody)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `system med azure-token uten rollen access_as_application gir 401`() {
        client.post().uri("/system/hent-identer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureToken(claims = emptyMap())}")
            .header(X_CORRELATION_ID, "system-uten-rolle")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(hentIdenterBody)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
