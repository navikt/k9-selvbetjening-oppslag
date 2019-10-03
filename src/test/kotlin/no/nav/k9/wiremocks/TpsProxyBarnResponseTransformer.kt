package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import no.nav.k9.utgaende.rest.NavHeaders

class TpsProxyBarnResponseTransformer : ResponseTransformer() {
    override fun transform(
        request: Request?,
        response: Response?,
        files: FileSource?,
        parameters: Parameters?
    ): Response {
        val personIdent = request!!.getHeader(NavHeaders.PersonIdent)

        return Response.Builder.like(response)
            .body(getResponse(personIdent))
            .build()
    }

    override fun getName(): String {
        return "tps-proxy-barn"
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}

private fun getResponse(navIdent: String) : String {
    when (navIdent) {
        "10047025546" -> {
            return """
            [
          {
            "datoFraOgMed": null,
            "kilde": null,
            "ident": "11121279632",
            "relasjonsType": {
              "verdi": "BARN",
              "kodeverk": "Familierelasjoner"
            },
            "forkortetNavn": "KONSOLL KLØKTIG BLUNKENDE",
            "kjoenn": "K",
            "foedselsdato": "2012-12-11",
            "alder": 6,
            "harSammeAdresse": true,
            "statsborgerskap": {
              "datoFraOgMed": "2012-12-11",
              "kilde": "SKD",
              "kode": {
                "verdi": "NOR",
                "kodeverk": "StatsborgerskapFreg"
              }
            },
            "doedsdato": null,
            "spesiellOpplysning": null,
            "egenansatt": {
              "datoFraOgMed": null,
              "kilde": null,
              "erEgenansatt": false
            }
          },
          {
            "datoFraOgMed": null,
            "kilde": null,
            "ident": "24121479490",
            "relasjonsType": {
              "verdi": "BARN",
              "kodeverk": "Familierelasjoner"
            },
            "forkortetNavn": "HEST SLAPP OVERSTRÅLENDE",
            "kjoenn": "K",
            "foedselsdato": "2014-12-24",
            "alder": 4,
            "harSammeAdresse": true,
            "statsborgerskap": {
              "datoFraOgMed": "2014-12-24",
              "kilde": "SKD",
              "kode": {
                "verdi": "NOR",
                "kodeverk": "StatsborgerskapFreg"
              }
            },
            "doedsdato": null,
            "spesiellOpplysning": null,
            "egenansatt": {
              "datoFraOgMed": null,
              "kilde": null,
              "erEgenansatt": false
            }
          }
        ]
            """.trimIndent()
        } else -> {
        return """
            []
        """.trimIndent()
    }
    }
}


