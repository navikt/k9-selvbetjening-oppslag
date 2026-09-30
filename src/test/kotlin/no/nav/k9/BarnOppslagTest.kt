package no.nav.k9

import no.nav.k9.PersonFødselsnummer.PERSON_1_MED_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_2_MED_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_3_MED_SKJERMET_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_4_MED_DØD_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_UTEN_BARN
import no.nav.k9.TokenUtils.hentToken
import no.nav.k9.integrasjon.common.NavHeaders
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.json.JsonCompareMode
import org.springframework.http.MediaType

class BarnOppslagTest : ApplicationTestBase() {

    @Test
    fun `test barnOppslag aktoerId`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_2_MED_BARN)
        val expectedResponse = """
        { 
            "barn":[
                {"aktør_id":"65432"}
            ]
        }
        """.trimIndent()
        client.get().uri("/meg?a=barn[].aktør_id")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "barn-oppslag-aktoer-id")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            //feiler. AktørId for barn blir satt til forelders aktørId
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `test barnOppslag navn og fødselsdato`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_2_MED_BARN)
        // Første barn har totalt navn over > 24 tegn, så gjøres eget oppslag på navnet, den andre unngår oppslag da den er <= 24 tegn
        //language=json
        val expectedResponse = """
        { 
            "barn":[
                {
                    "fornavn": "TALENTFULL",
                    "mellomnavn": "MELLOMROM",
                    "etternavn": "STAUDE",
                    "fødselsdato": "2017-03-18"
                }
            ]
        }
        """.trimIndent()
        client.get().uri("/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn&a=barn[].fødselsdato")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "barn-oppslag-navn-foedselsdato")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `test barnOppslag navn har ikke mellomnavn`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        val expectedResponse = """
        { 
            "barn":[
                {
                    "fornavn": "OLA",
                    "etternavn": "NORDMANN"
                }
            ]
        }
        """.trimIndent()
        client.get().uri("/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "barn-oppslag-har-ikke-mellomnavn")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `gitt barn med strengt fortrolig adresse, forvent tom liste`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_3_MED_SKJERMET_BARN)
        val expectedResponse = """
        { 
            "barn": []
        }
        """.trimIndent()
        client.get().uri("/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "barn-oppslag-har-ikke-mellomnavn")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `gitt død barn, forvent tom liste`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_4_MED_DØD_BARN)
        val expectedResponse = """
        { 
            "barn": []
        }
        """.trimIndent()
        client.get().uri("/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "barn-oppslag-har-ikke-mellomnavn")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `test barnOppslag ingenBarn`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_UTEN_BARN)
        val expectedResponse = """
        { 
            "barn":[]
        }
        """.trimIndent()
        client.get().uri("/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "barn-oppslag-ingen-barn")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }
}
