package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import no.nav.k9.BarnFødselsnummer.BARN_TIL_PERSON_1
import no.nav.k9.BarnFødselsnummer.BARN_TIL_PERSON_2
import no.nav.k9.PersonFødselsnummer
import no.nav.k9.PersonFødselsnummer.PERSON_1_MED_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_2_MED_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_3_MED_SKJERMET_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_4_MED_DØD_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_MED_FLERE_ARBEIDSFORHOLD_PER_ARBEIDSGIVER
import no.nav.k9.PersonFødselsnummer.PERSON_MED_FLERE_ROLLER_I_FORETAK
import no.nav.k9.PersonFødselsnummer.PERSON_MED_FORETAK
import no.nav.k9.PersonFødselsnummer.PERSON_MED_FRILANS_OPPDRAG
import no.nav.k9.PersonFødselsnummer.PERSON_UTEN_ARBEIDSGIVER
import no.nav.k9.PersonFødselsnummer.PERSON_UTEN_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_UTEN_FORETAK
import no.nav.siftilgangskontroll.core.pdl.utils.pdlHentIdenterResponse
import no.nav.siftilgangskontroll.pdl.generated.enums.IdentGruppe
import no.nav.siftilgangskontroll.pdl.generated.hentident.IdentInformasjon
import org.json.JSONObject
import org.slf4j.LoggerFactory

private val identMap = mapOf(
    PERSON_1_MED_BARN to IdentInformasjon(ident = "12345", gruppe = IdentGruppe.AKTORID, historisk = false),
    PERSON_2_MED_BARN to IdentInformasjon(ident = "23456", gruppe = IdentGruppe.AKTORID, historisk = false),
    PERSON_UTEN_BARN to IdentInformasjon(ident = "34567", gruppe = IdentGruppe.AKTORID, historisk = false),
    PERSON_3_MED_SKJERMET_BARN to IdentInformasjon(ident = "111111", gruppe = IdentGruppe.AKTORID, historisk = false),
    PERSON_4_MED_DØD_BARN to IdentInformasjon(ident = "222222", gruppe = IdentGruppe.AKTORID, historisk = false),
    BARN_TIL_PERSON_1 to IdentInformasjon(ident = "54321", gruppe = IdentGruppe.AKTORID, historisk = false),
    BARN_TIL_PERSON_2 to IdentInformasjon(ident = "65432", gruppe = IdentGruppe.AKTORID, historisk = false),
    PERSON_UTEN_FORETAK to IdentInformasjon(ident = "13579", gruppe = IdentGruppe.AKTORID, historisk = false),
    PERSON_MED_FORETAK to IdentInformasjon(ident = "246801", gruppe = IdentGruppe.AKTORID, historisk = false),
    PERSON_MED_FLERE_ROLLER_I_FORETAK to IdentInformasjon(ident = "573028", gruppe = IdentGruppe.AKTORID, historisk = false),
    PERSON_UTEN_ARBEIDSGIVER to IdentInformasjon(ident = "485028", gruppe = IdentGruppe.AKTORID, historisk = false),
    PERSON_MED_FLERE_ARBEIDSFORHOLD_PER_ARBEIDSGIVER to IdentInformasjon(ident = "485030", gruppe = IdentGruppe.AKTORID, historisk = false),
    PERSON_MED_FRILANS_OPPDRAG to IdentInformasjon(ident = "485031", gruppe = IdentGruppe.AKTORID, historisk = false)
)


class PdlAktoerIdResponseTransformer : ResponseTransformer() {
    private companion object {
        val logger = LoggerFactory.getLogger(PdlAktoerIdResponseTransformer::class.java)
    }

    override fun transform(
        request: Request?,
        response: Response?,
        files: FileSource?,
        parameters: Parameters?,
    ): Response {

        val requestBody = JSONObject(request!!.body.decodeToString())
        val requestVariables = requestBody.getJSONObject("variables")
        val ident = requestVariables.getString("ident")
        logger.info("Hentet ident fra request: {}", ident)

        return Response.Builder.like(response)
            .body(getResponse(ident))
            .build()
    }

    override fun getName(): String {
        return "pdl-hent-ident"
    }

    override fun applyGlobally(): Boolean {
        return false
    }

}

private fun getResponse(ident: String) = pdlHentIdenterResponse(listOf(identMap[ident]!!))
