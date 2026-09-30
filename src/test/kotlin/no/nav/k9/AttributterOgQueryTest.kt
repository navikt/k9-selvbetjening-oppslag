package no.nav.k9

import no.nav.k9.PersonFødselsnummer.PERSON_1_MED_BARN
import no.nav.k9.TokenUtils.hentToken
import no.nav.k9.integrasjon.common.NavHeaders
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode
import java.net.URI

class AttributterOgQueryTest : ApplicationTestBase() {

    @Test
    fun `test oppslag bare ugyldig attributt - bad request`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        val expectedResponse = """
        {
            "detail":"Requesten inneholder ugyldige parametere.",
            "instance":"/meg",
            "type":"/problem-details/invalid-request-parameters",
            "title":"invalid-request-parameters",
            "violations":[
                {"parameterName":"a","parameterType":"QUERY","reason":"Er ikke en støttet attributt.","invalidValue":"ugyldigattrib"}
            ],
            "status":400
        }
        """.trimIndent()
        client.get().uri("/meg?a=ugyldigAttrib")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "oppslag-ugyldig-attrib")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `test oppslag ugyldige attributt - bad request`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        val expectedResponse = """
        {
            "detail":"Requesten inneholder ugyldige parametere.",
            "instance":"/meg",
            "type":"/problem-details/invalid-request-parameters",
            "title":"invalid-request-parameters",
            "violations":[
                {"parameterName":"a","parameterType":"QUERY","reason":"Er ikke en støttet attributt.","invalidValue":"ugyldigattrib"},
                {"parameterName":"a","parameterType":"QUERY","reason":"Er ikke en støttet attributt.","invalidValue":"annetugyldigattrib"}
            ],
            "status":400
        }
        """.trimIndent()
        client.get().uri("/meg?a=aktør_id&a=ugyldigattrib&a=fornavn&a=annetugyldigattrib")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "oppslag-ugyldige-attrib")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().json(expectedResponse, JsonCompareMode.STRICT)
    }

    @Test
    fun `attributter er case-insensitive, blanke filtreres bort og duplikater fjernes`() {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get().uri("/meg?a=&a= &a=AKTØR_ID&a=aktør_id")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "attributter-case")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json("""{ "aktør_id": "12345" }""", JsonCompareMode.STRICT)
    }

    @Test
    fun `prosentenkodet query gir samme svar som ukodet`() {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get().uri(URI.create("http://localhost:$port/meg?a=akt%C3%B8r_id&a=arbeidsgivere%5B%5D.organisasjoner%5B%5D.organisasjonsnummer"))
            .header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            .header(X_CORRELATION_ID, "prosentenkodet-query")
            .header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json("""{ "aktør_id": "12345", "arbeidsgivere": { "organisasjoner": [ { "organisasjonsnummer": "123456789" } ] } }""", JsonCompareMode.STRICT)
    }

    @Test
    fun `rå hakeparenteser i query uten prosent-enkoding godtas`() {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        val (statusLinje, body) = rawGet(
            "/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer",
            "Authorization" to "Bearer $idToken",
            X_CORRELATION_ID to "raa-query-tegn",
            NavHeaders.XK9Ytelse to "${Ytelse.PLEIEPENGER_SYKT_BARN}"
        )
        assertTrue(statusLinje.contains(" 200"), "Statuslinje var $statusLinje")
        JSONAssert.assertEquals(
            """{"arbeidsgivere":{"organisasjoner":[{"organisasjonsnummer":"123456789"}]}}""",
            body,
            true
        )
    }

    private fun rawGet(pathAndQuery: String, vararg headers: Pair<String, String>): Pair<String, String> =
        java.net.Socket("localhost", port).use { socket ->
            val forespørsel = buildString {
                append("GET $pathAndQuery HTTP/1.1\r\nHost: localhost:$port\r\nConnection: close\r\n")
                headers.forEach { (navn, verdi) -> append("$navn: $verdi\r\n") }
                append("\r\n")
            }
            socket.getOutputStream().apply { write(forespørsel.toByteArray(Charsets.UTF_8)); flush() }
            val svar = socket.getInputStream().readBytes().toString(Charsets.UTF_8)
            val hode = svar.substringBefore("\r\n\r\n")
            val kropp = svar.substringAfter("\r\n\r\n")
            val erChunked = hode.lines().any { it.equals("Transfer-Encoding: chunked", ignoreCase = true) }
            svar.substringBefore("\r\n") to if (erChunked) kropp.dekodChunked() else kropp
        }

    private fun String.dekodChunked(): String = buildString {
        var rest = this@dekodChunked
        while (true) {
            val lengde = rest.substringBefore("\r\n").trim().toInt(16)
            if (lengde == 0) break
            rest = rest.substringAfter("\r\n")
            append(rest.substring(0, lengde))
            rest = rest.substring(lengde).removePrefix("\r\n")
        }
    }
}
