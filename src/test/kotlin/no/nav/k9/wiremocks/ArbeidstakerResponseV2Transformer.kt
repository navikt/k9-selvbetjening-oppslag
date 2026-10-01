package no.nav.k9.wiremocks

import tools.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import no.nav.k9.PersonFødselsnummer
import no.nav.k9.integrasjon.common.NavHeaders

class ArbeidstakerResponseV2Transformer : ResponseTransformerV2 {

    override fun getName(): String {
        return "arbeidstaker-arbeidsforhold-v2"
    }

    override fun transform(response: Response, serveEvent: ServeEvent): Response {
        val personIdent = serveEvent.request.getHeader(NavHeaders.PersonIdent)

        return Response.Builder.like(response)
            .body(getResponse(personIdent))
            .build()
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}

private val objectMapper = jacksonObjectMapper()

private fun getResponse(navIdent: String): String {
    val ansettelsesperiode = mapOf("startdato" to "2020-01-01", "sluttdato" to "2029-02-28")
    val ansettelsesperiode2 = mapOf("startdato" to "2015-01-01", "sluttdato" to "2019-12-31")
    val identerOrg = listOf(mapOf("ident" to "123456789", "type" to "ORGANISASJONSNUMMER"))
    val identerFolkeregistrert = listOf(mapOf("ident" to "28837996386", "type" to "FOLKEREGISTERIDENT"))
    val arbeidsstedUnderenhet = mapOf("type" to "Underenhet", "identer" to identerOrg)
    val arbeidsstedPerson = mapOf("type" to "Person", "identer" to identerFolkeregistrert)

    fun arbeidsforhold(type: String, ansettelsesperiode: Map<String, String>, arbeidssted: Map<String, Any>) = mapOf(
        "type" to mapOf("kode" to type),
        "ansettelsesperiode" to ansettelsesperiode,
        "arbeidssted" to arbeidssted
    )

    val ordinært = "ordinaertArbeidsforhold"
    val frilans = "frilanserOppdragstakerHonorarPersonerMm"
    val privatArbeidsgiverUnderenhet = arbeidsforhold(ordinært, ansettelsesperiode, arbeidsstedUnderenhet)
    val privatArbeidsgiverPerson = arbeidsforhold(ordinært, ansettelsesperiode, arbeidsstedPerson)
    val organisasjon = arbeidsforhold(ordinært, ansettelsesperiode, arbeidsstedUnderenhet)
    val frilansoppdragPerson = arbeidsforhold(frilans, ansettelsesperiode, arbeidsstedPerson)
    val frilansoppdragUnderenhet = arbeidsforhold(frilans, ansettelsesperiode, arbeidsstedUnderenhet)

    val respons = when (navIdent) {
        PersonFødselsnummer.PERSON_1_MED_BARN -> listOf(
            privatArbeidsgiverPerson,
            privatArbeidsgiverUnderenhet,
            organisasjon,
            frilansoppdragPerson,
            frilansoppdragUnderenhet
        )

        PersonFødselsnummer.PERSON_MED_FRILANS_OPPDRAG -> listOf(frilansoppdragPerson, frilansoppdragUnderenhet)

        PersonFødselsnummer.PERSON_MED_FLERE_ARBEIDSFORHOLD_PER_ARBEIDSGIVER -> listOf(
            privatArbeidsgiverPerson,
            privatArbeidsgiverPerson,
            organisasjon,
            arbeidsforhold(ordinært, ansettelsesperiode2, arbeidsstedUnderenhet)
        )

        else -> emptyList()
    }
    return objectMapper.writeValueAsString(respons)
}
