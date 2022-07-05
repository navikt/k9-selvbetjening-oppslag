package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import no.nav.k9.PersonFødselsnummer
import no.nav.k9.utgaende.rest.NavHeaders
import org.json.JSONObject

class ArbeidstakerResponseV2Transformer : ResponseTransformer() {
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
        return "arbeidstaker-arbeidsforhold-v2"
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}

private fun getResponse(navIdent: String) : String {
    when (navIdent) {
        PersonFødselsnummer.PERSON_MED_FRILANS_OPPDRAG -> {
            //language=json
            return """
                [
                  {
                    "arbeidssted": {
                      "identer": [
                        {
                          "ident": "2716992950188",
                          "type": "AKTORID"
                        },
                        {
                          "ident": "805824352",
                          "type": "FOLKEREGISTERIDENT"
                        }
                      ],
                      "type": "Person"
                    },
                    "type": {
                      "kode": "frilanserOppdragstakerHonorarPersonerMm"
                    },
                    "ansettelsesperiode": {
                      "startdato": "2020-01-01",
                      "sluttdato": "2022-02-28"
                    }
                  },
                  {
                    "arbeidssted": {
                      "identer": [
                        {
                          "ident": "123456789",
                          "type": "ORGANISASJONSNUMMER"
                        }
                      ],
                      "type": "Underenhet"
                    },
                    "type": {
                      "kode": "frilanserOppdragstakerHonorarPersonerMm"
                    },
                    "ansettelsesperiode": {
                      "startdato": "2020-01-01",
                      "sluttdato": "2022-02-28"
                    }
                  }
                ]
            """.trimIndent()
        }
        PersonFødselsnummer.PERSON_MED_FLERE_ARBEIDSFORHOLD_PER_ARBEIDSGIVER -> {
            //language=json
            return """
                [
                  {
                    "arbeidssted": {
                      "identer": [
                        {
                          "ident": "2716992950188",
                          "type": "AKTORID"
                        },
                        {
                          "ident": "28837996386",
                          "type": "FOLKEREGISTERIDENT"
                        }
                      ],
                      "type": "Person"
                    },
                    "type": {
                      "kode": "frilanserOppdragstakerHonorarPersonerMm"
                    },
                    "ansettelsesperiode": {
                      "startdato": "2002-07-05"
                    }
                  },
                  {
                    "arbeidssted": {
                      "identer": [
                        {
                          "ident": "2716992950188",
                          "type": "AKTORID"
                        },
                        {
                          "ident": "28837996386",
                          "type": "FOLKEREGISTERIDENT"
                        }
                      ],
                      "type": "Person"
                    },
                    "type": {
                      "kode": "ordinaertArbeidsforhold"
                    },
                    "ansettelsesperiode": {
                      "startdato": "2002-07-05"
                    }
                  },
                  {
                    "arbeidssted": {
                      "identer": [
                        {
                          "ident": "2716992950188",
                          "type": "AKTORID"
                        },
                        {
                          "ident": "28837996386",
                          "type": "FOLKEREGISTERIDENT"
                        }
                      ],
                      "type": "Person"
                    },
                    "type": {
                      "kode": "ordinaertArbeidsforhold"
                    },
                    "ansettelsesperiode": {
                      "startdato": "2002-07-05",
                      "sluttdato": "2021-07-01"
                    }
                  },
                  {
                    "arbeidssted": {
                      "identer": [
                        {
                          "ident": "2984883819834",
                          "type": "AKTORID"
                        },
                        {
                          "ident": "08816898316",
                          "type": "FOLKEREGISTERIDENT"
                        }
                      ],
                      "type": "Person"
                    },
                    "type": {
                      "kode": "ordinaertArbeidsforhold"
                    },
                    "ansettelsesperiode": {
                      "startdato": "2002-07-05"
                    }
                  },
                  {
                    "arbeidssted": {
                      "identer": [
                        {
                          "ident": "839942907",
                          "type": "ORGANISASJONSNUMMER"
                        }
                      ],
                      "type": "Underenhet"
                    },
                    "type": {
                      "kode": "frilanserOppdragstakerHonorarPersonerMm"
                    },
                    "ansettelsesperiode": {
                      "startdato": "2002-07-05"
                    }
                  },
                  {
                    "arbeidssted": {
                      "identer": [
                        {
                          "ident": "896929119",
                          "type": "ORGANISASJONSNUMMER"
                        }
                      ],
                      "type": "Underenhet"
                    },
                    "type": {
                      "kode": "ordinaertArbeidsforhold"
                    },
                    "ansettelsesperiode": {
                      "startdato": "2002-07-05"
                    }
                  }
                ]
            """.trimIndent()
        }
        else -> {
            return """
                 []
             """.trimIndent()
        }
    }
}