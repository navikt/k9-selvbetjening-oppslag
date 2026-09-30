package no.nav.k9

import no.nav.k9.PersonFødselsnummer.PERSON_1_MED_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_MED_FLERE_ARBEIDSFORHOLD_PER_ARBEIDSGIVER
import no.nav.k9.PersonFødselsnummer.PERSON_MED_FRILANS_OPPDRAG
import no.nav.k9.PersonFødselsnummer.PERSON_UTEN_ARBEIDSGIVER
import no.nav.k9.TokenUtils.hentToken
import no.nav.k9.integrasjon.common.NavHeaders
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode

class ArbeidsgiverOppslagTest : ApplicationTestBase() {

    @Test
    fun `test arbeidsgiverOppslag orgnr`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        val expectedResponse = """
        {
            "arbeidsgivere": {
                "organisasjoner": [
                    {
                    "organisasjonsnummer": "123456789"
                    }
                ]
            }
         }
        """.trimIndent()
        client.get().uri("/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "arbeidsgiver-oppslag-orgnr")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(JSON_UTF8)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `test arbeidsgiverOppslag orgnr og navn`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        val expectedResponse = """
    {
        "arbeidsgivere": {
            "organisasjoner": [
                {
                    "organisasjonsnummer": "123456789",
                    "navn": "DNB, FORSIKRING"
                }
            ]
        }
     }
    """.trimIndent()
        client.get().uri("/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "arbeidsgiver-oppslag-orgnr-navn")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(JSON_UTF8)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `Forvent organiasjon uten navn, gitt at navn ikke er funnet`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        //language=json
        val expectedResponse = """
    {
        "arbeidsgivere": {
            "organisasjoner": [
                {
                    "organisasjonsnummer": "11111111"
                }
            ]
        }
     }
    """.trimIndent()
        client.get().uri("/arbeidsgivere?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn&org=11111111")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "arbeidsgiver-oppslag-orgnr-navn")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(JSON_UTF8)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `Forvent 1 organisasjon med navn, gitt organisasjonsnummer`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        //language=json
        val expectedResponse = """
    {
        "arbeidsgivere": {
            "organisasjoner": [
                {
                    "organisasjonsnummer": "981585216",
                    "navn": "NAV FAMILIE- OG PENSJONSYTELSER"
                }
            ]
        }
     }
    """.trimIndent()
        client.get().uri("/arbeidsgivere?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn&org=981585216")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "arbeidsgiver-oppslag-orgnr-navn")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(JSON_UTF8)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `Forvent 2 organisasjoner med navn, gitt organisasjonsnummer`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        //language=json
        val expectedResponse = """
    {
        "arbeidsgivere": {
            "organisasjoner": [
                {
                    "organisasjonsnummer": "981585216",
                    "navn": "NAV FAMILIE- OG PENSJONSYTELSER"
                },
                {
                    "organisasjonsnummer": "67564534",
                    "navn": "SELSKAP, MED, VELDIG, MANGE, NAVNELINJER"
                }
            ]
        }
     }
    """.trimIndent()
        client.get().uri("/arbeidsgivere?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn&org=981585216&org=67564534")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "arbeidsgiver-oppslag-orgnr-navn")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(JSON_UTF8)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `test arbeidsgiverOppslag orgnr, navn, fom og tom`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        //language=json
        val expectedResponse = """
            {
              "arbeidsgivere": {
                "organisasjoner": [
                  {
                    "ansatt_fom": "2020-01-01",
                    "ansatt_tom": "2029-02-28",
                    "navn": "DNB, FORSIKRING",
                    "organisasjonsnummer": "123456789"
                  }
                ]
              }
            }
        """.trimIndent()
        client.get().uri("/meg?fom=2019-02-02&tom=2023-10-10&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn&a=arbeidsgivere[].organisasjoner[].ansettelsesperiode")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "arbeidsgiver-oppslag-orgnr-navn")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(JSON_UTF8)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `test arbeidsgiverOppslag med ingen arbeidsgivere`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_UTEN_ARBEIDSGIVER)
        val expectedResponse = """
    {
        "arbeidsgivere":{
            "organisasjoner":[],
            "private_arbeidsgivere":[]
        }
    }
    """.trimIndent()
        client.get().uri("/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn" +
                "&a=private_arbeidsgivere[].offentlig_ident&a=private_arbeidsgivere[].ansettelsesperiode")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "arbeidsgiver-oppslag-ingen-arbeidsgiver")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(JSON_UTF8)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `tester oppslag av private arbeidsgivere`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        val expectedResponse = """
            {
              "arbeidsgivere": {
                "private_arbeidsgivere": [
                  {
                    "offentlig_ident": "28837996386",
                    "ansatt_fom": "2020-01-01",
                    "ansatt_tom": "2029-02-28"
                  }
                ]
              }
            }
            """.trimIndent()
        client.get().uri("/meg?a=private_arbeidsgivere[].offentlig_ident&a=private_arbeidsgivere[].ansettelsesperiode")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "arbeidsgiver-oppslag-private-arbeidsgivere")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(JSON_UTF8)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `Forventer å kun få unike arbeidsgivere selvom man har flere arbeidsforhold hos en arbeidsgiver`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_MED_FLERE_ARBEIDSFORHOLD_PER_ARBEIDSGIVER)
        val expectedResponse = """
            {
              "arbeidsgivere": {
                "private_arbeidsgivere": [
                  {
                    "ansatt_fom": "2020-01-01",
                    "offentlig_ident": "28837996386",
                    "ansatt_tom": "2029-02-28"
                  }
                ],
                "organisasjoner": [
                  {
                    "navn": "DNB, FORSIKRING",
                    "organisasjonsnummer": "123456789"
                  }
                ]
              }
            }
            """.trimIndent()
        client.get().uri("/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn" +
                "&a=private_arbeidsgivere[].offentlig_ident&a=private_arbeidsgivere[].ansettelsesperiode")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "arbeidsgiver-oppslag-arbeidsgivere")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(JSON_UTF8)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `Forventer flere ansettelsesperioder hos arbeidsgiver`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_MED_FLERE_ARBEIDSFORHOLD_PER_ARBEIDSGIVER)
        val expectedResponse = """
            {
              "arbeidsgivere": {
                "organisasjoner": [
                  {
                    "navn": "DNB, FORSIKRING",
                    "organisasjonsnummer": "123456789",
                    "ansatt_fom": "2015-01-01",
                    "ansatt_tom": "2019-12-31"
                  },
                  {
                    "navn": "DNB, FORSIKRING",
                    "organisasjonsnummer": "123456789",
                    "ansatt_fom": "2020-01-01",
                    "ansatt_tom": "2029-02-28"
                  }
                ]
              }
            }
            """.trimIndent()
        client.get().uri("/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn&a=arbeidsgivere[].organisasjoner[].ansettelsesperiode&inkluderAlleAnsettelsesperioder=true&fom=2014-01-01")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "arbeidsgiver-oppslag-arbeidsgivere")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(JSON_UTF8)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `Teste oppslag av frilans oppdrag`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_MED_FRILANS_OPPDRAG)
        val expectedResponse = """
            {
              "arbeidsgivere": {
                "frilansoppdrag": [
                  {
                    "type": "Person",
                    "ansatt_fom": "2020-01-01",
                    "ansatt_tom": "2029-02-28",
                    "offentlig_ident": "28837996386"
                  },
                  {
                    "type": "Organisasjon",
                    "ansatt_fom": "2020-01-01",
                    "ansatt_tom": "2029-02-28",
                    "organisasjonsnummer": "123456789",
                    "navn": "DNB, FORSIKRING"
                  }
                ]
              }
            }
            """.trimIndent()
        client.get().uri("/meg?a=frilansoppdrag[]")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "arbeidsgiver-oppslag-frilans-oppdrag")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(JSON_UTF8)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `test arbeidsgiverOppslag feil format fom`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        val expectedResponse = """
        {
            "detail":"Requesten inneholder ugyldige parametere.",
            "instance":"/meg",
            "type":"/problem-details/invalid-request-parameters",
            "title":"invalid-request-parameters",
            "violations":[
                {"parameterName":"fom","parameterType":"QUERY","reason":"Må være på format yyyy-mm-dd.","invalidValue":"2019/02/02"}
            ],
            "status":400
        }
        """.trimIndent()
        client.get().uri("/meg?fom=2019/02/02&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "oppslag-feil-format-fom")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `test arbeidsgiverOppslag feil format tom`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        val expectedResponse = """
        {
            "detail":"Requesten inneholder ugyldige parametere.",
            "instance":"/meg",
            "type":"/problem-details/invalid-request-parameters",
            "title":"invalid-request-parameters",
            "violations":[
                {"parameterName":"tom","parameterType":"QUERY","reason":"Må være på format yyyy-mm-dd.","invalidValue":"2019.10.10"}
            ],
            "status":400
        }
        """.trimIndent()
        client.get().uri("/meg?fom=2019-02-02&tom=2019.10.10&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "oppslag-feil-format-tom")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `arbeidsgivere-endepunktet gir organisasjoner`() {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get().uri("/arbeidsgivere?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&org=981585216")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "arbeidsgivere-endepunkt")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(JSON_UTF8)
            .expectBody().json("""{ "arbeidsgivere": { "organisasjoner": [ { "organisasjonsnummer": "981585216" } ] } }""", JsonCompareMode.STRICT)
    }
}
