package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import no.nav.k9.PersonFødselsnummer.PERSON_1_MED_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_2_MED_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_3_MED_SKJERMET_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_4_MED_DØD_BARN
import no.nav.siftilgangskontroll.core.pdl.utils.pdlHentIdenterBolkResponse
import no.nav.siftilgangskontroll.pdl.generated.enums.IdentGruppe
import no.nav.siftilgangskontroll.pdl.generated.hentidenterbolk.HentIdenterBolkResult
import no.nav.siftilgangskontroll.pdl.generated.hentidenterbolk.IdentInformasjon
import org.json.JSONObject
import org.slf4j.LoggerFactory

private val identerMap = mapOf(
    PERSON_1_MED_BARN to HentIdenterBolkResult(
        ident = PERSON_1_MED_BARN,
        identer = listOf(IdentInformasjon(
            ident = PERSON_1_MED_BARN,
            gruppe = IdentGruppe.FOLKEREGISTERIDENT
        )),
        code = "ok"
    ),

    PERSON_2_MED_BARN to HentIdenterBolkResult(
        ident = PERSON_2_MED_BARN,
        identer = listOf(IdentInformasjon(
            ident = PERSON_2_MED_BARN,
            gruppe = IdentGruppe.FOLKEREGISTERIDENT
        )),
        code = "ok"
    ),

    PERSON_3_MED_SKJERMET_BARN to HentIdenterBolkResult(
        ident = PERSON_3_MED_SKJERMET_BARN,
        identer = listOf(IdentInformasjon(
            ident = PERSON_3_MED_SKJERMET_BARN,
            gruppe = IdentGruppe.FOLKEREGISTERIDENT
        )),
        code = "ok"
    ),

    PERSON_4_MED_DØD_BARN to HentIdenterBolkResult(
        ident = PERSON_4_MED_DØD_BARN,
        identer = listOf(IdentInformasjon(
            ident = PERSON_4_MED_DØD_BARN,
            gruppe = IdentGruppe.FOLKEREGISTERIDENT
        )),
        code = "ok"
    ),
)

class PDLHentIdentBolkResponseTransformer : ResponseTransformerV2 {
    private companion object {
        val logger = LoggerFactory.getLogger(PDLHentPersonBolkResponseTransformer::class.java)
    }

    override fun getName(): String {
        return "pdl-hent-identer-bolk"
    }

    override fun transform(response: Response, serveEvent: ServeEvent): Response {
        val requestBody = JSONObject(serveEvent.request.body.decodeToString())
        val identer: List<String> = requestBody.getJSONObject("variables").getJSONArray("identer").map {
            it as String
        }
        logger.info("Hentet identer fra request: {}", identer)

        return Response.Builder.like(response)
            .body(getResponse(identer))
            .build()
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}

private fun getResponse(identer: List<String>): String {
    return pdlHentIdenterBolkResponse(identer.map {
        identerMap[it]!!
    })
}
