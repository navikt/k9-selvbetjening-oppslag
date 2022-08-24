package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import no.nav.k9.PersonFødselsnummer
import no.nav.k9.utgaende.rest.NavHeaders
import org.json.JSONArray
import org.json.JSONObject

class ArbeidstakerResponseV2Transformer : ResponseTransformer() {
    override fun transform(
        request: Request?,
        response: Response?,
        files: FileSource?,
        parameters: Parameters?
    ): Response {
        val personIdent = request!!.getHeader(NavHeaders.PersonIdent)

        return Response.Builder.like(response)
            .body(getResponse(personIdent))
            .build()
    }

    override fun getName(): String {
        return "arbeidstaker-arbeidsforhold-v2"
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}

private fun getResponse(navIdent: String) : String {
    val jsonRespons = JSONArray()
    val ansettelsesperiode = JSONObject().apply { put("startdato","2020-01-01"); put("sluttdato", "2029-02-28") }
    val identerOrg = JSONArray().apply { put(JSONObject().apply { put("ident", "123456789"); put("type", "ORGANISASJONSNUMMER") }) }
    val identerFolkeregistrert = JSONArray().apply { put(JSONObject().apply { put("ident", "28837996386"); put("type", "FOLKEREGISTERIDENT") }) }
    val arbeidsstedUnderenhet = JSONObject().apply { put("type", "Underenhet"); put("identer", identerOrg) }
    val arbeidsstedPerson = JSONObject().apply { put("type", "Person"); put("identer", identerFolkeregistrert) }
    val privatArbeidsgiverUnderenhet = JSONObject().apply {
        put("type", JSONObject().apply { put("kode", "ordinaertArbeidsforhold")})
        put("ansettelsesperiode", ansettelsesperiode)
        put("arbeidssted", arbeidsstedUnderenhet)
    }
    val privatArbeidsgiverPerson = JSONObject().apply {
        put("type", JSONObject().apply { put("kode", "ordinaertArbeidsforhold")})
        put("ansettelsesperiode", ansettelsesperiode)
        put("arbeidssted", arbeidsstedPerson)
    }
    val organisasjon = JSONObject().apply {
        put("type", JSONObject().apply { put("kode", "ordinaertArbeidsforhold")})
        put("ansettelsesperiode", ansettelsesperiode)
        put("arbeidssted", arbeidsstedUnderenhet)
    }
    val frilansoppdragPerson = JSONObject().apply {
        put("type", JSONObject().apply { put("kode", "frilanserOppdragstakerHonorarPersonerMm")})
        put("ansettelsesperiode", ansettelsesperiode)
        put("arbeidssted", arbeidsstedPerson)
    }
    val frilansoppdragUnderenhet = JSONObject().apply {
        put("type", JSONObject().apply { put("kode", "frilanserOppdragstakerHonorarPersonerMm")})
        put("ansettelsesperiode", ansettelsesperiode)
        put("arbeidssted", arbeidsstedUnderenhet)
    }
    when (navIdent) {
        PersonFødselsnummer.PERSON_1_MED_BARN -> {
            return jsonRespons.apply {
                put(privatArbeidsgiverPerson)
                put(privatArbeidsgiverUnderenhet)
                put(organisasjon)
                put(frilansoppdragPerson)
                put(frilansoppdragUnderenhet)
            }.toString()
        }
        PersonFødselsnummer.PERSON_MED_FRILANS_OPPDRAG -> {
            return jsonRespons.apply {
                put(frilansoppdragPerson)
                put(frilansoppdragUnderenhet)
            }.toString()
        }
        PersonFødselsnummer.PERSON_MED_FLERE_ARBEIDSFORHOLD_PER_ARBEIDSGIVER -> {
            return jsonRespons.apply {
                put(privatArbeidsgiverPerson)
                put(privatArbeidsgiverPerson)
                put(organisasjon)
                put(organisasjon)
            }.toString()
        }
        else -> {
            return jsonRespons.toString()
        }
    }
}