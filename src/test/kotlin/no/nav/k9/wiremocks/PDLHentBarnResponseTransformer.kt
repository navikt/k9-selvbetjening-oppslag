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
import no.nav.k9.clients.pdl.generated.hentperson.Navn
import no.nav.siftilgangskontroll.pdl.generated.enums.AdressebeskyttelseGradering
import no.nav.siftilgangskontroll.pdl.generated.hentbarn.Folkeregisteridentifikator
import no.nav.siftilgangskontroll.pdl.generated.hentperson.Doedsfall
import no.nav.siftilgangskontroll.pdl.generated.hentperson.Foedsel
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory

private val barnMap = mapOf(
    BARN_TIL_PERSON_1 to JSONObject(mapOf(
        "navn" to listOf(Navn(
            fornavn = "OLA",
            mellomnavn = null,
            etternavn = "NORDMANN",
            forkortetNavn = "OLA NORDMANN",
        )).navnSomJsonArray(),
        "fødselsdato" to listOf(Foedsel("2012-02-24")).fødselSomJsonArray(),
        "folkeregisteridentifikator" to listOf(Folkeregisteridentifikator(BARN_TIL_PERSON_1)).folkeregisteridentifikatorSomJsonArray(),
        "adressebeskyttelse" to listOf<AdressebeskyttelseGradering>().adressebeskyttelseGraderingJsonArray(),
        "doedsfall" to listOf<Doedsfall>().dødsfallSomJsonArray()
    )),

    BARN_TIL_PERSON_2 to JSONObject(mapOf(
        "navn" to listOf(Navn(
            fornavn = "TALENTFULL",
            mellomnavn = "MELLOMROM",
            etternavn = "STAUDE",
            forkortetNavn = "TALENTFULL MELLOMROM STAUDE",
        )).navnSomJsonArray(),
        "fødselsdato" to listOf(Foedsel("2017-03-18")).fødselSomJsonArray(),
        "folkeregisteridentifikator" to listOf(Folkeregisteridentifikator(BARN_TIL_PERSON_2)).folkeregisteridentifikatorSomJsonArray(),
        "adressebeskyttelse" to listOf<AdressebeskyttelseGradering>().adressebeskyttelseGraderingJsonArray(),
        "doedsfall" to listOf<Doedsfall>().dødsfallSomJsonArray()
    )),

    SKJERMET_BARN_TIL_PERSON_3 to JSONObject(mapOf(
        "navn" to listOf(Navn(
            fornavn = "TVILSOM",
            mellomnavn = "GRADERT",
            etternavn = "VEPS",
            forkortetNavn = "TVILSOM GRADERT VEPS",
        )).navnSomJsonArray(),
        "fødselsdato" to listOf(Foedsel("2012-10-27")).fødselSomJsonArray(),
        "folkeregisteridentifikator" to listOf(Folkeregisteridentifikator(SKJERMET_BARN_TIL_PERSON_3)).folkeregisteridentifikatorSomJsonArray(),
        "adressebeskyttelse" to listOf(AdressebeskyttelseGradering.STRENGT_FORTROLIG).adressebeskyttelseGraderingJsonArray(),
        "doedsfall" to listOf<Doedsfall>().dødsfallSomJsonArray()
    )),

    DØD_BARN_TIL_PERSON_4 to JSONObject(mapOf(
        "navn" to listOf(Navn(
            fornavn = "Død",
            mellomnavn = "",
            etternavn = "BARN",
            forkortetNavn = "Død Barn",
        )).navnSomJsonArray(),
        "fødselsdato" to listOf(Foedsel("2012-10-27")).fødselSomJsonArray(),
        "folkeregisteridentifikator" to listOf(Folkeregisteridentifikator(DØD_BARN_TIL_PERSON_4)).folkeregisteridentifikatorSomJsonArray(),
        "adressebeskyttelse" to listOf<AdressebeskyttelseGradering>().adressebeskyttelseGraderingJsonArray(),
        "doedsfall" to listOf(Doedsfall("2020-06-01")).dødsfallSomJsonArray()
    )),
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

    val x = JSONObject(mapOf(
        "data" to JSONObject(mapOf(
            "hentPersonBolk" to JSONArray(identer.map {
                val barn = barnMap[it]!!
                val navn = barn.getJSONArray("navn")
                val fødselsdato = barn.getJSONArray("fødselsdato")
                val folkeregisteridentifikator: JSONArray = barn.getJSONArray("folkeregisteridentifikator")
                val adressebeskyttelse: JSONArray = barn.getJSONArray("adressebeskyttelse")
                val dødsfall: JSONArray = barn.getJSONArray("doedsfall")

                JSONObject(mapOf(
                    "ident" to it,
                    "person" to JSONObject(mapOf(
                        "folkeregisteridentifikator" to folkeregisteridentifikator,
                        "navn" to navn,
                        "foedsel" to fødselsdato,
                        "doedsfall" to dødsfall,
                        "adressebeskyttelse" to adressebeskyttelse
                    )),
                    "code" to "ok"
                ))
            })
        ))
    ))
    return x.toString()
}
