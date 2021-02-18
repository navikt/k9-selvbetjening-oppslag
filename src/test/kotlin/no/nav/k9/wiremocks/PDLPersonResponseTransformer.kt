package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import no.nav.k9.utgaende.rest.NavHeaders
import org.json.JSONObject
import org.slf4j.LoggerFactory

class PDLPersonResponseTransformer : ResponseTransformer() {
    private companion object {
        val logger = LoggerFactory.getLogger(PDLPersonResponseTransformer::class.java)
    }

    override fun transform(
        request: Request?,
        response: Response?,
        files: FileSource?,
        parameters: Parameters?
    ): Response {
        val requestBody = JSONObject(request!!.body.decodeToString())
        val ident = requestBody.getJSONObject("variables").getString("ident")
        logger.info("Hentet ident fra request: {}", ident)

        return Response.Builder.like(response)
            .body(getResponse(ident))
            .build()
    }

    override fun getName(): String {
        return "pdl-person"
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}

private fun getResponse(navIdent: String) : String {
    val fornavn: String
    val mellomnavn: String
    val etternavn: String
    val forkortetNavn: String
    val foedselsdato: String

    when (navIdent) {
        "01019012345" -> {
            fornavn = "STOR-KAR"
            mellomnavn = "LANGEMANN"
            etternavn = "TEST"
            forkortetNavn = "$fornavn $mellomnavn $etternavn"
            foedselsdato = "1985-07-27"
        } "25037139184" -> {
            fornavn = "ARNE"
            mellomnavn = "BJARNE"
            etternavn = "CARLSEN"
            forkortetNavn = "$fornavn $mellomnavn $etternavn"
            foedselsdato = "1990-01-02"
        } else -> {
            fornavn = "CATO"
            mellomnavn = ""
            etternavn = "NILSEN"
            forkortetNavn = "$fornavn $etternavn"
            foedselsdato = "1980-05-20"
        }
    }
    //language=json
    return """
    {
        "data": {
            "hentPerson": {
                "navn": [
                    {
                        "fornavn": "$fornavn",
                        "mellomnavn": "$mellomnavn",
                        "etternavn": "$etternavn",
                        "forkortetNavn": "$forkortetNavn"
                    }
                ],
                "folkeregisteridentifikator": [
                    {
                        "identifikasjonsnummer": "$navIdent"
                    }
                ],
                "foedsel": [
                    {
                        "foedselsdato": "$foedselsdato"
                    }
                ]
            }
        }
    }
    """.trimIndent()
}


