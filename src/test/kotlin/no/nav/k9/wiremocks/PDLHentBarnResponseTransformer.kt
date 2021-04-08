package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import no.nav.k9.utgaende.rest.getStringOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory

private val barnMap = mapOf(
    "11129998665" to JSONObject(mapOf(
        "fornavn" to "OLA",
        "mellomnavn" to null,
        "etternavn" to "NORDMANN",
        "forkortetNavn" to "OLA NORDMANN",
        "fødselsdato" to "2012-02-24"
    )),
    "01010067894" to JSONObject(mapOf(
        "fornavn" to "TALENTFULL",
        "mellomnavn" to "MELLOMROM",
        "etternavn" to "STAUDE",
        "forkortetNavn" to "TALENTFULL MELLOMROM STAUDE",
        "fødselsdato" to "2017-03-18"
    )),
    "24021982330" to JSONObject(mapOf(
        "fornavn" to "LUGUBER",
        "mellomnavn" to null,
        "etternavn" to "SKILPADDE",
        "forkortetNavn" to "SKILPADDE LUGUBER",
        "fødselsdato" to "2019-02-24"
    )),
    "27101274832" to JSONObject(mapOf(
        "fornavn" to "TVILSOM",
        "mellomnavn" to "GRADERT",
        "etternavn" to "VEPS",
        "forkortetNavn" to "TVILSOM GRADERT VEPS",
        "fødselsdato" to "2012-10-27"
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
        logger.info("Hentet ident fra request: {}", identer)

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

    return JSONObject(mapOf(
        "data" to JSONObject(mapOf(
            "hentPersonBolk" to JSONArray(identer.map {
                val barn = barnMap[it]!!
                val fornavn = barn.getString("fornavn")
                val mellomnavn = barn.getStringOrNull("mellomnavn")
                val etternavn = barn.getString("etternavn")
                val fortkortetNavn = barn.getString("forkortetNavn")
                val fødselsdato = barn.getString("fødselsdato")

                JSONObject(mapOf(
                    "ident" to it,
                    "person" to JSONObject(mapOf(
                        "navn" to JSONArray(listOf(mapOf(
                            "fornavn" to fornavn,
                            "mellomnavn" to mellomnavn,
                            "etternavn" to etternavn,
                            "fortkortetNavn" to fortkortetNavn
                        ))),
                        "foedsel" to JSONArray(listOf(JSONObject(mapOf(
                            "foedselsdato" to fødselsdato
                        )))),
                        "doedsfall" to JSONArray(),
                        "adressebeskyttelse" to
                                if (it == "27101274832") JSONArray(listOf(JSONObject(
                                    mapOf("gradering" to "STRENGT_FORTROLIG")
                                ))) else JSONArray()
                    )),
                    "code" to "ok"
                ))
            })
        ))
    )).toString()
}


/*"""
          {
               "ident": "$ident",
               "person": {
                   "navn": [
                       {
                           "fornavn": "$fornavn",
                           "mellomnavn": "$mellomnavn",
                           "etternavn": "$etternavn",
                           "forkortetNavn": "$fortkortetNavn"
                       }
                   ],
                   "foedsel": [
                       {
                           "foedselsdato": ""
                       }
                   ],
                   "doedsfall": []
               },
               "code": "ok"
           },
                 """.trimIndent()
   }*/
