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
        parameters: Parameters?,
    ): Response {
        val requestBody = JSONObject(request!!.body.decodeToString())
        val ident = requestBody.getJSONObject("variables").getString("ident")
        logger.info("Hentet ident fra request: {}", ident)

        return Response.Builder.like(response)
            .body(getResponse(ident))
            .build()
    }

    override fun getName(): String {
        return "pdl-hent-person"
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}

private fun getResponse(navIdent: String): String {
    // søker
    val fornavn: String
    val mellomnavn: String
    val etternavn: String
    val forkortetNavn: String
    val foedselsdato: String

    // barn
    val relatertPersonsIdent: String?
    val relatertPersonsRolle: String?
    val minRolleForPerson: String?


    when (navIdent) {
        "01019012345" -> {
            fornavn = "STOR-KAR"
            mellomnavn = "LANGEMANN"
            etternavn = "TEST"
            forkortetNavn = "$fornavn $mellomnavn $etternavn"
            foedselsdato = "1985-07-27"

            relatertPersonsIdent = "11129998665"
            relatertPersonsRolle = "BARN"
            minRolleForPerson = "MOR"

        }
        "25037139184" -> {
            fornavn = "ARNE"
            mellomnavn = "BJARNE"
            etternavn = "CARLSEN"
            forkortetNavn = "$fornavn $mellomnavn $etternavn"
            foedselsdato = "1990-01-02"

            relatertPersonsIdent = "01010067894"
            relatertPersonsRolle = "BARN"
            minRolleForPerson = "MOR"

        }
        else -> {
            fornavn = "CATO"
            mellomnavn = ""
            etternavn = "NILSEN"
            forkortetNavn = "$fornavn $etternavn"
            foedselsdato = "1980-05-20"

            relatertPersonsIdent = null
            relatertPersonsRolle = null
            minRolleForPerson = null
        }
    }

    //language=json
    return when (relatertPersonsIdent) {
        null -> """
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
                        ],
                        "familierelasjoner": [],
                        "forelderBarnRelasjon": []
                    }
                }
            }
    """.trimIndent()

        else -> """
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
                        ],
                        "familierelasjoner": [
                            {
                                "relatertPersonsIdent": "$relatertPersonsIdent",
                                "relatertPersonsRolle": "$relatertPersonsRolle",
                                "minRolleForPerson": "$minRolleForPerson"
                            }
                        ],
                        "forelderBarnRelasjon": [
                            {
                                "relatertPersonsIdent": "$relatertPersonsIdent",
                                "relatertPersonsRolle": "$relatertPersonsRolle",
                                "minRolleForPerson": "$minRolleForPerson"
                            }
                          ]
                    }
                }
            }
    """.trimIndent()
    }
}


