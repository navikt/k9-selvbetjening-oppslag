package no.nav.k9

import no.nav.k9.BarnFødselsnummer.BARN_TIL_PERSON_1
import no.nav.k9.PersonFødselsnummer.PERSON_1_MED_BARN
import no.nav.k9.integrasjon.common.NavHeaders
import no.nav.siftilgangskontroll.pdl.generated.enums.IdentGruppe
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.servlet.client.expectBody
import java.util.*

class SystemOppslagTest : ApplicationTestBase() {

    @Test
    fun `test systemoppslag uten XCorrelationId gir BadRequest`() {
        val azureToken = mockOAuth2Server.issueToken(
            issuerId = "azure",
            subject = UUID.randomUUID().toString(),
            audience = "dev-fss:dusseldorf:k9-selvbetjening-oppslag",
            claims = mapOf("roles" to "access_as_application")
        ).serialize()

        client.post().uri("/system/hent-identer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $azureToken")
            .header(HttpHeaders.ACCEPT, "application/json")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            //language=json
            .body(
                """
                {
                    "identer": ["$PERSON_1_MED_BARN"],
                    "identGrupper": ["${IdentGruppe.FOLKEREGISTERIDENT}"]
                }
            """.trimIndent()
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `systemoppslag med azure token skal gi 200`() {
        val azureToken = mockOAuth2Server.issueToken(
            issuerId = "azure",
            subject = UUID.randomUUID().toString(),
            audience = "dev-fss:dusseldorf:k9-selvbetjening-oppslag",
            claims = mapOf("roles" to "access_as_application")
        ).serialize()

        //language=json
        val expectedResponse = """
            [
              {
                "code": "ok",
                "ident": "$PERSON_1_MED_BARN",
                "identer": [
                  {
                    "ident": "$PERSON_1_MED_BARN",
                    "gruppe": "${IdentGruppe.FOLKEREGISTERIDENT}"
                  }
                ]
              }
            ]
        """.trimIndent()
        client.post().uri("/system/hent-identer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $azureToken")
            .header(X_CORRELATION_ID, "systemoppslag-hent-identer")
            .header(HttpHeaders.ACCEPT, "application/json")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            //language=json
            .body(
                """
                {
                    "identer": ["$PERSON_1_MED_BARN"],
                    "identGrupper": ["${IdentGruppe.FOLKEREGISTERIDENT}"]
                }
            """.trimIndent()
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `systemoppslag ignorerer ukjente felt i body`() {
        val azureToken = mockOAuth2Server.issueToken(
            issuerId = "azure",
            subject = UUID.randomUUID().toString(),
            audience = "dev-fss:dusseldorf:k9-selvbetjening-oppslag",
            claims = mapOf("roles" to "access_as_application")
        ).serialize()

        //language=json
        val expectedResponse = """
            [
              {
                "code": "ok",
                "ident": "$PERSON_1_MED_BARN",
                "identer": [
                  {
                    "ident": "$PERSON_1_MED_BARN",
                    "gruppe": "${IdentGruppe.FOLKEREGISTERIDENT}"
                  }
                ]
              }
            ]
        """.trimIndent()
        client.post().uri("/system/hent-identer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $azureToken")
            .header(X_CORRELATION_ID, "systemoppslag-ukjent-felt")
            .header(HttpHeaders.ACCEPT, "application/json")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            //language=json
            .body(
                """
                {
                    "ukjentFelt": "verdi",
                    "identer": ["$PERSON_1_MED_BARN"],
                    "identGrupper": ["${IdentGruppe.FOLKEREGISTERIDENT}"]
                }
            """.trimIndent()
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `systemoppslag for å hente barn `() {
        val azureToken = mockOAuth2Server.issueToken(
            issuerId = "azure",
            subject = UUID.randomUUID().toString(),
            audience = "dev-fss:dusseldorf:k9-selvbetjening-oppslag",
            claims = mapOf("roles" to "access_as_application")
        ).serialize()

        //language=json
        val expectedResponse = """
            [
              {
                  "aktørId": {
                    "value": "54321"
                  },
                  "pdlBarn": {
                    "fornavn": "OLA",
                    "etternavn": "NORDMANN",
                    "ident": {
                      "value": "${BarnFødselsnummer.BARN_TIL_PERSON_1}"
                    },
                    "fødselsdato": "2012-02-24",
                    "adressebeskyttelse": []
                  }
              }
            ]
        """.trimIndent()
        client.post().uri("/system/hent-barn")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $azureToken")
            .header(X_CORRELATION_ID, "systemoppslag-hent-barn")
            .header(HttpHeaders.ACCEPT, "application/json")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            //language=json
            .body(
                """
                {
                    "identer": ["${BarnFødselsnummer.BARN_TIL_PERSON_1}"]
                }
            """.trimIndent()
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `systemoppslag for å hente adressebeskyttet barn `() {
        val azureToken = mockOAuth2Server.issueToken(
            issuerId = "azure",
            subject = UUID.randomUUID().toString(),
            audience = "dev-fss:dusseldorf:k9-selvbetjening-oppslag",
            claims = mapOf("roles" to "access_as_application")
        ).serialize()

        //language=json
        val expectedResponse = """
            [
              {
                  "aktørId": {
                    "value": "666666"
                  },
                  "pdlBarn": {
                    "fornavn": "TVILSOM",
                    "mellomnavn": "GRADERT",
                    "etternavn": "VEPS",
                    "ident": {
                      "value": "${BarnFødselsnummer.SKJERMET_BARN_TIL_PERSON_3}"
                    },
                    "fødselsdato": "2012-10-27",
                    "adressebeskyttelse": [
                        {
                            "gradering": "STRENGT_FORTROLIG"
                        }
                    ]
                  }
              }
            ]
        """.trimIndent()
        client.post().uri("/system/hent-barn")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $azureToken")
            .header(X_CORRELATION_ID, "systemoppslag-hent-adresebeskyttet-barn")
            .header(HttpHeaders.ACCEPT, "application/json")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            //language=json
            .body(
                """
                {
                    "identer": ["${BarnFødselsnummer.SKJERMET_BARN_TIL_PERSON_3}"]
                }
            """.trimIndent()
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `system med ugyldig eller tom body gir 400`() {
        listOf("ikke json", "", "{}").forEach { body ->
            client.post().uri("/system/hent-identer")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureToken()}")
                .header(X_CORRELATION_ID, "system-ugyldig-body")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(body)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
        }
    }

    @Test
    fun `system hent-barn uten X-K9-Ytelse gir 500`() {
        client.post().uri("/system/hent-barn")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureToken()}")
            .header(X_CORRELATION_ID, "system-hent-barn-uten-ytelse")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body("""{ "identer": ["$BARN_TIL_PERSON_1"] }""")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `system ignorerer ukjente felter i request body`() {
        client.post().uri("/system/hent-identer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${azureToken()}")
            .header(X_CORRELATION_ID, "system-ukjent-felt")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .body(
                """
                {
                    "identer": ["$PERSON_1_MED_BARN"],
                    "identGrupper": ["${IdentGruppe.FOLKEREGISTERIDENT}"],
                    "ukjentFelt": "ignoreres"
                }
                """.trimIndent()
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
    }
}
