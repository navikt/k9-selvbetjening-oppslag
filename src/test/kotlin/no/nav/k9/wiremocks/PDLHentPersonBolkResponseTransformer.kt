package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import org.json.JSONObject
import org.slf4j.LoggerFactory

private val barnMap = mapOf(
    "11129998665" to JSONObject(mapOf(
        "fornavn" to "LUGUBER",
        "mellomnavn" to null,
        "etternavn" to "SKILPADDE",
        "forkortetNavn" to "SKILPADDE LUGUBER",
        "barnFødselsdato" to "2019-02-24",
        "barnIdent" to "24021982330"
    )),
    "01010067894" to JSONObject(mapOf(
        "fornavn" to "TALENTFULL",
        "mellomnavn" to "MELLOMROM",
        "etternavn" to "STAUDE",
        "barnForkortetNavn" to "TALENTFULL MELLOMROM STAUDE",
        "barnFødselsdato" to "2017-03-18",
        "barnIdent" to "18031779975"
    )),
    "24021982330" to JSONObject(mapOf(
        "fornavn" to "LUGUBER",
        "mellomnavn" to null,
        "etternavn" to "SKILPADDE",
        "barnForkortetNavn" to "SKILPADDE LUGUBER",
        "barnFødselsdato" to "2019-02-24",
        "barnIdent" to "24021982330"
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
        return "pdl-hent-person-bolk"
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}

private fun getResponse(identer: List<String>): String {
    //language=json
    return """
    {
        "data": {
          "hentPersonBolk": ${
        when {
            identer.isEmpty() -> """[]"""
            else ->
                """
                  ${person(identer)}
            """.trimIndent()
        }
    }
}
    """.trimIndent()
}

fun person(identer: List<String>): List<String> = identer.map {
    val barn = barnMap[it]!!
    val fornavn = barn.getString("fornavn")
    val mellomnavn = barn.getString("mellomnavn")
    val etternavn = barn.getString("etternavn")
    val fortkortetNavn = barn.getString("barnForkortetNavn")
    val ident = barn.getString("barnIdent")

    barn.toString()

    //language=json

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
