package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import no.nav.k9.utgaende.rest.NavHeaders

private val identMap = mapOf(
    "01019012345" to "12345",
    "25037139184" to "23456",
    "10047025546" to "34567",
    "11121279632" to "54321", // barn av 10047025546
    "24121479490" to "65432" // barn av 10047025546
)

class AktoerRegisterResponseTransformer : ResponseTransformer() {
    override fun transform(
        request: Request?,
        response: Response?,
        files: FileSource?,
        parameters: Parameters?
    ): Response {
        val personIdent = request!!.getHeader(NavHeaders.PersonIdenter)
        val identGruppe = request.queryParameter("identgruppe").firstValue()

        return Response.Builder.like(response)
            .body(getResponse(
                personIdent = personIdent,
                identGruppe = identGruppe
            ))
            .build()
    }

    override fun getName(): String {
        return "aktoer-register"
    }

    override fun applyGlobally(): Boolean {
        return false
    }

}

private fun getResponse(
    personIdent: String,
    identGruppe: String
) = """
{
  "$personIdent": {
    "identer": [
      {
        "ident": "${identMap[personIdent]}",
        "identgruppe": "$identGruppe",
        "gjeldende": true
      }
    ],
    "feilmelding": null
  }
}
""".trimIndent()
