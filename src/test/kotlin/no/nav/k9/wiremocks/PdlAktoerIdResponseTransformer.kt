package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import no.nav.k9.BarnFødselsnummer.BARN_TIL_PERSON_1
import no.nav.k9.BarnFødselsnummer.BARN_TIL_PERSON_2
import org.json.JSONObject
import org.slf4j.LoggerFactory

private val identMap = mapOf(
    "01019012345" to "12345",
    "25037139184" to "23456",
    "10047025546" to "34567",
    BARN_TIL_PERSON_1 to "54321", // barn av 01019012345
    BARN_TIL_PERSON_2 to "65432", // barn av 25037139184
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

        val identGruppe = requestVariables.getJSONArray("grupper").first() as String

        return Response.Builder.like(response)
            .body(getResponse(
                personIdent = ident,
                identGruppe = identGruppe
            ))
            .build()
    }

    override fun getName(): String {
        return "pdl-hent-ident"
    }

    override fun applyGlobally(): Boolean {
        return false
    }

}

private fun getResponse(
    personIdent: String,
    identGruppe: String,
) =
    //language=json
    """
{
    "data": {
        "hentIdenter": {
            "identer": [
                {
                    "ident": "${identMap[personIdent]}",
                    "historisk": false,
                    "gruppe": ": $identGruppe"
                }
            ]
        }
    }
}
""".trimIndent()
