package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import no.nav.k9.BarnFødselsnummer.BARN_TIL_PERSON_1
import no.nav.k9.BarnFødselsnummer.BARN_TIL_PERSON_2
import no.nav.k9.BarnFødselsnummer.DØD_BARN_TIL_PERSON_4
import no.nav.k9.BarnFødselsnummer.SKJERMET_BARN_TIL_PERSON_3
import no.nav.siftilgangskontroll.core.pdl.utils.*
import no.nav.siftilgangskontroll.pdl.generated.enums.AdressebeskyttelseGradering
import no.nav.siftilgangskontroll.pdl.generated.hentbarn.*
import org.json.JSONObject
import org.slf4j.LoggerFactory

private val barnMap = mapOf(
    BARN_TIL_PERSON_1 to Person(
        folkeregisteridentifikator = listOf(Folkeregisteridentifikator(BARN_TIL_PERSON_1)),
        navn = listOf(Navn(
            fornavn = "OLA",
            mellomnavn = null,
            etternavn = "NORDMANN",
            forkortetNavn = "OLA NORDMANN",
        )),
        foedsel = listOf(Foedsel("2012-02-24")),
        doedsfall = listOf(),
        adressebeskyttelse = listOf()
    ),

    BARN_TIL_PERSON_2 to Person(
        folkeregisteridentifikator = listOf(Folkeregisteridentifikator(BARN_TIL_PERSON_2)),
        navn = listOf(Navn(
            fornavn = "TALENTFULL",
            mellomnavn = "MELLOMROM",
            etternavn = "STAUDE",
            forkortetNavn = "TALENTFULL MELLOMROM STAUDE",
        )),
        foedsel = listOf(Foedsel("2017-03-18")),
        doedsfall = listOf(),
        adressebeskyttelse = listOf()
    ),

    SKJERMET_BARN_TIL_PERSON_3 to Person(
        folkeregisteridentifikator = listOf(Folkeregisteridentifikator(SKJERMET_BARN_TIL_PERSON_3)),
        navn = listOf(Navn(
            fornavn = "TVILSOM",
            mellomnavn = "GRADERT",
            etternavn = "VEPS",
            forkortetNavn = "TVILSOM GRADERT VEPS",
        )),
        foedsel = listOf(Foedsel("2012-10-27")),
        doedsfall = listOf(),
        adressebeskyttelse = listOf(Adressebeskyttelse(AdressebeskyttelseGradering.STRENGT_FORTROLIG))
    ),

    DØD_BARN_TIL_PERSON_4 to Person(
        folkeregisteridentifikator = listOf(Folkeregisteridentifikator(DØD_BARN_TIL_PERSON_4)),
        navn = listOf(Navn(
            fornavn = "Død",
            mellomnavn = "",
            etternavn = "BARN",
            forkortetNavn = "Død Barn",
        )),
        foedsel = listOf(Foedsel("2012-10-27")),
        doedsfall = listOf(Doedsfall("2020-06-01")),
        adressebeskyttelse = listOf()
    ),
)

class PDLHentPersonBolkResponseTransformer : ResponseTransformer() {
    private companion object {
        val logger = LoggerFactory.getLogger(PDLHentPersonBolkResponseTransformer::class.java)
    }

    override fun transform(
        request: Request?,
        response: Response?,
        files: FileSource?,
        parameters: Parameters?,
    ): Response {
        val requestBody = JSONObject(request!!.body.decodeToString())
        val identer: List<String> = requestBody.getJSONObject("variables").getJSONArray("identer").map {
            it as String
        }
        logger.info("Hentet barnIdenter fra request: {}", identer)

        return Response.Builder.like(response)
            .body(getResponse(identer))
            .build()
    }

    override fun getName(): String {
        return "pdl-hent-barn"
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}

private fun getResponse(identer: List<String>): String {
    return pdlHentPersonBolkResponse(identer.map {
        HentPersonBolkResult(
            person = barnMap[it],
            code = "200"
        )
    })
}
