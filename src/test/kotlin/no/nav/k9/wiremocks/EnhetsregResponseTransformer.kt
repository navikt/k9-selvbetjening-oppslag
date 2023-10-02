package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent

class EnhetsregResponseTransformer : ResponseTransformerV2 {
    override fun getName(): String {
        return "enhetsreg-noekkelinfo"
    }

    override fun transform(response: Response, serveEvent: ServeEvent): Response {
        val requestPathChunks = serveEvent.request.url.split("/")
        val orgnr = requestPathChunks[requestPathChunks.size-2]
        return Response.Builder.like(response)
            .body(getResponse(orgnr))
            .build()
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}

private fun getResponse(organisasjonsnummer: String) : String {
    //language=json
    return when (organisasjonsnummer) {
        "981585216" -> """
        {
          "adresse": {
            "adresselinje1": "string",
            "adresselinje2": "string",
            "adresselinje3": "string",
            "bruksperiode": {
              "fom": "2015-01-06T21:44:04.748",
              "tom": "2015-12-06T19:45:04"
            },
            "gyldighetsperiode": {
              "fom": "2014-07-01",
              "tom": "2015-12-31"
            },
            "kommunenummer": "0301",
            "landkode": "JPN",
            "postnummer": "0557",
            "poststed": "string"
          },
          "enhetstype": "BEDR",
          "navn": {
            "bruksperiode": {
              "fom": "2015-01-06T21:44:04.748",
              "tom": "2015-12-06T19:45:04"
            },
            "gyldighetsperiode": {
              "fom": "2014-07-01",
              "tom": "2015-12-31"
            },
            "navnelinje1": "NAV FAMILIE- OG PENSJONSYTELSER",
            "navnelinje2": "",
            "navnelinje3": "",
            "navnelinje4": "",
            "navnelinje5": "",
            "redigertnavn": "NAV FAMILIE- OG PENSJONSYTELSER OSL"
          },
          "opphoersdato": "2016-12-31",
          "organisasjonsnummer": "981585216"
        }
        """.trimIndent()
        "123456789" -> """
        {
          "adresse": {
            "adresselinje1": "",
            "adresselinje2": "",
            "adresselinje3": "",
            "bruksperiode": {
              "fom": "2016-01-06T21:44:04.748",
              "tom": "2016-12-06T19:45:04"
            },
            "gyldighetsperiode": {
              "fom": "2015-07-01",
              "tom": "2016-12-31"
            },
            "kommunenummer": "0301",
            "landkode": "NOR",
            "postnummer": "0557",
            "poststed": "string"
          },
          "enhetstype": "BEDR",
          "navn": {
            "bruksperiode": {
              "fom": "2016-01-06T21:44:04.748",
              "tom": "2016-12-06T19:45:04"
            },
            "gyldighetsperiode": {
              "fom": "2015-07-01",
              "tom": "2016-12-31"
            },
            "navnelinje1": "DNB",
            "navnelinje2": "FORSIKRING",
            "redigertnavn": "DNB HELSE- OG LIVSFORSIKRING"
          },
          "opphoersdato": "2017-12-31",
          "organisasjonsnummer": "123456789"
        }
        """.trimIndent()
        "67564534" -> """
        {
          "adresse": {
            "adresselinje1": "",
            "adresselinje2": "",
            "adresselinje3": "",
            "bruksperiode": {
              "fom": "2016-01-06T21:44:04.748",
              "tom": "2016-12-06T19:45:04"
            },
            "gyldighetsperiode": {
              "fom": "2015-07-01",
              "tom": "2016-12-31"
            },
            "kommunenummer": "0301",
            "landkode": "NOR",
            "postnummer": "0557",
            "poststed": "string"
          },
          "enhetstype": "BEDR",
          "navn": {
            "bruksperiode": {
              "fom": "2016-01-06T21:44:04.748",
              "tom": "2016-12-06T19:45:04"
            },
            "gyldighetsperiode": {
              "fom": "2015-07-01",
              "tom": "2016-12-31"
            },
            "navnelinje1": "SELSKAP",
            "navnelinje2": "MED",
            "navnelinje3": "VELDIG",
            "navnelinje4": "MANGE",
            "navnelinje5": "NAVNELINJER",
            "redigertnavn": "TEIT SELSKAP"
          },
          "opphoersdato": "2017-12-31",
          "organisasjonsnummer": "67564534"
        }
        """.trimIndent()
        "11111111" -> """
        {
          "adresse": {
            "adresselinje1": "",
            "adresselinje2": "",
            "adresselinje3": "",
            "bruksperiode": {
              "fom": "2016-01-06T21:44:04.748",
              "tom": "2016-12-06T19:45:04"
            },
            "gyldighetsperiode": {
              "fom": "2015-07-01",
              "tom": "2016-12-31"
            },
            "kommunenummer": "0301",
            "landkode": "NOR",
            "postnummer": "0557",
            "poststed": "string"
          },
          "enhetstype": "BEDR",
          "navn": {
            "bruksperiode": {
              "fom": "2016-01-06T21:44:04.748",
              "tom": "2016-12-06T19:45:04"
            },
            "gyldighetsperiode": {
              "fom": "2015-07-01",
              "tom": "2016-12-31"
            },
            "navnelinje1": null,
            "navnelinje2": null,
            "navnelinje3": null,
            "navnelinje4": null,
            "navnelinje5": null,
            "redigertnavn": null
          },
          "opphoersdato": "2017-12-31",
          "organisasjonsnummer": "11111111"
        }
        """.trimIndent()
        "1" -> minimalResponse("1", "ENK")
        "2" -> minimalResponse("2", "DA")
        "3" -> minimalResponse("3", "ANS", opphørsdato = "2020-06-01")
        else -> "{}"
    }
}

private fun minimalResponse(
    organisasjonsnummer: String,
    enhetstype: String,
    opphørsdato: String?= null
) = """
{
    "organisasjonsnummer": "$organisasjonsnummer",
    "navn": {
        "navnlinje1": "$enhetstype",
        "navnlinje2": "$organisasjonsnummer"
    },
    ${if(opphørsdato != null) """
        "opphoersdato": "$opphørsdato",
    """.trimIndent() else ""} 
    "enhetstype": "$enhetstype"
}
""".trimIndent()
