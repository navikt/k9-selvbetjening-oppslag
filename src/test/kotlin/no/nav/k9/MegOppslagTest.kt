package no.nav.k9

import no.nav.k9.PersonFødselsnummer.DØD_PERSON
import no.nav.k9.PersonFødselsnummer.PERSON_1_MED_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_2_MED_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_UNDER_MYNDIGHETS_ALDER
import no.nav.k9.TokenUtils.hentToken
import no.nav.k9.integrasjon.common.NavHeaders
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode

class MegOppslagTest : ApplicationTestBase() {

    @Test
    fun `test megOppslag aktoerId`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        val expectedResponse = """
        { "aktør_id": "12345" }
        """.trimIndent()
        client.get().uri("/meg?a=aktør_id")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "meg-oppslag-aktoer-id")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `test megOppslag aktør_id og fornavn`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_2_MED_BARN)
        val expectedResponse = """
        { "aktør_id": "23456",
         "fornavn": "ARNE"}
        """.trimIndent()
        client.get().uri("/meg?a=aktør_id&a=fornavn")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "meg-oppslag-aktoer-id-fornavn")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `test megOppslag aktør_id og navn og fødselsdato`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_2_MED_BARN)
        val expectedResponse = """
        { 
            "aktør_id": "23456",
            "fornavn": "ARNE",
            "mellomnavn": "BJARNE",
            "etternavn": "CARLSEN",
            "fødselsdato": "1990-01-02"
        }
        """.trimIndent()
        client.get().uri("/meg?a=aktør_id&a=fornavn&a=mellomnavn&a=etternavn&a=fødselsdato")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "meg-oppslag-aktoer-id-navn-foedselsdato")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `test megOppslag navn har ikke mellomnavn`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = "01010067894")
        val expectedResponse = """
        {
            "fornavn": "CATO",
            "mellomnavn": "",
            "etternavn": "NILSEN"
        }
        """.trimIndent()
        client.get().uri("/meg?a=fornavn&a=mellomnavn&a=etternavn")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "meg-oppslag-har-ikke-mellomnavn")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `gitt oppslag av død person, forvent feil`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = DØD_PERSON)
        //language=json
        val expectedResponse = """
        {
            "detail": "Policy decision: DENY - Reason: (NAV-bruker er ikke lenger i live AND NAV-bruker er myndig)",
            "instance": "/meg",
            "type": "/problem-details/tilgangskontroll-feil",
            "title": "tilgangskontroll-feil",
            "status": 451
        }
        """.trimIndent()
        client.get().uri("/meg?a=fornavn&a=mellomnavn&a=etternavn")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "meg-oppslag-dod-person")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(451)
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `gitt oppslag av person under myndighetsalder (18), forvent 451 Unavailable For Legal Reasons`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_UNDER_MYNDIGHETS_ALDER)
    //language=json
    val expectedResponse = """
        {
            "detail": "Policy decision: DENY - Reason: (NAV-bruker er i live AND NAV-bruker er ikke myndig)",
            "instance": "/meg",
            "type": "/problem-details/tilgangskontroll-feil",
            "title": "tilgangskontroll-feil",
            "status": 451
        }
        """.trimIndent()
        client.get().uri("/meg?a=fornavn&a=mellomnavn&a=etternavn")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "meg-oppslag-under-myndighet")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(451)
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `test oppslag alle attributter`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        val expectedResponse = """
            {
              "mellomnavn": "LANGEMANN",
              "etternavn": "TEST",
              "arbeidsgivere": {
                "private_arbeidsgivere": [
                  {
                    "ansatt_fom": "2020-01-01",
                    "offentlig_ident": "28837996386",
                    "ansatt_tom": "2029-02-28"
                  }
                ],
                "frilansoppdrag": [
                  {
                    "ansatt_fom": "2020-01-01",
                    "offentlig_ident": "28837996386",
                    "type": "Person",
                    "ansatt_tom": "2029-02-28"
                  },
                  {
                    "ansatt_fom": "2020-01-01",
                    "navn": "DNB, FORSIKRING",
                    "type": "Organisasjon",
                    "organisasjonsnummer": "123456789",
                    "ansatt_tom": "2029-02-28"
                  }
                ],
                "organisasjoner": [
                  {
                    "navn": "DNB, FORSIKRING",
                    "organisasjonsnummer": "123456789"
                  }
                ]
              },
              "barn": [
                {
                  "etternavn": "NORDMANN",
                  "identitetsnummer": "11129998665",
                  "fødselsdato": "2012-02-24",
                  "fornavn": "OLA"
                }
              ],
              "fødselsdato": "1985-07-27",
              "fornavn": "STOR-KAR",
              "aktør_id": "12345"
            }
    """.trimIndent()
        client.get().uri("/meg?fom=2019-09-09&tom=2022-10-10" +
                "&a=aktør_id&a=fornavn&a=mellomnavn&a=etternavn&a=fødselsdato" +
                "&a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn&a=barn[].fødselsdato&a=barn[].har_samme_adresse&a=barn[].identitetsnummer" +
                "&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn" +
                "&a=private_arbeidsgivere[].offentlig_ident&a=private_arbeidsgivere[].ansettelsesperiode&a=frilansoppdrag[]")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "oppslag-alle-attrib")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `test oppslag ingen attributter skal returnere tom JSON`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        val expectedResponse = """
        {}
        """.trimIndent()
        client.get().uri("/meg")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "oppslag-ingen-attrib")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `gitt oppslag av søker under myndighetsalder, forvent 451 Unavailable For Legal Reasons`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_UNDER_MYNDIGHETS_ALDER)
        //language=json
        val expectedResponse = """
        {
            "detail": "Policy decision: DENY - Reason: (NAV-bruker er i live AND NAV-bruker er ikke myndig)",
            "instance": "/meg",
            "type": "/problem-details/tilgangskontroll-feil",
            "title": "tilgangskontroll-feil",
            "status": 451
        }
        """.trimIndent()
        client.get().uri("/meg?a=aktør_id")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "oppslag-ugyldige-attrib")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(451)
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }
}
