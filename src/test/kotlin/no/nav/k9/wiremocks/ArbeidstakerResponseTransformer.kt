package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import no.nav.k9.PersonFødselsnummer
import no.nav.k9.utgaende.rest.NavHeaders

class ArbeidstakerResponseTransformer : ResponseTransformer() {
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
        return "arbeidstaker-arbeidsforhold"
    }

    override fun applyGlobally(): Boolean {
        return false
    }
}

private fun getResponse(navIdent: String) : String {
    when (navIdent) {
        PersonFødselsnummer.PERSON_MED_FLERE_ARBEIDSFORHOLD_PER_ARBEIDSGIVER -> {
            //language=json
            return """
                [
                  {
                    "arbeidsforholdId":"1",
                    "type":"ordinaertArbeidsforhold",
                    "ansettelsesperiode":{
                      "periode":{
                        "fom": "2014-07-01",
                        "tom": "2015-12-31"
                      }
                    },
                    "arbeidsgiver": {
                      "organisasjonsnummer": "981585216",
                      "type": "Organisasjon"
                    }
                  },
                  {
                    "arbeidsforholdId":"2",
                    "type":"ordinaertArbeidsforhold",
                    "ansettelsesperiode":{
                      "periode":{
                        "fom": "2014-07-02",
                        "tom": "2015-12-31"
                      }
                    },
                    "arbeidsgiver": {
                      "organisasjonsnummer": "981585216",
                      "type": "Organisasjon"
                    }
                  },
                  {
                    "arbeidsforholdId":"1",
                    "type":"ordinaertArbeidsforhold",
                    "ansettelsesperiode":{
                      "periode":{
                        "fom": "2014-07-01",
                        "tom": "2015-12-31"
                      }
                    },
                    "arbeidsgiver":{
                       "offentligIdent":"10047206508",
                       "aktoerId":"2142740417741",
                       "type":"Person"
                    }
                  },
                  {
                    "arbeidsforholdId":"2",
                    "type":"ordinaertArbeidsforhold",
                    "ansettelsesperiode":{
                      "periode":{
                        "fom": "2014-07-02",
                        "tom": "2015-12-31"
                      }
                    },
                    "arbeidsgiver":{
                       "offentligIdent":"10047206508",
                       "aktoerId":"2142740417741",
                       "type":"Person"
                    }
                  }
                ]
            """.trimIndent()
        }
        "01019012345" -> {
            //language=json
        return """
            [
              {
                "ansettelsesperiode": {
                  "bruksperiode": {
                    "fom": "2015-01-06T21:44:04.748",
                    "tom": "2015-12-06T19:45:04"
                  },
                  "periode": {
                    "fom": "2014-07-01",
                    "tom": "2015-12-31"
                  }
                },
                "arbeidsforholdId": "abc-321",
                "arbeidsgiver": {
                  "organisasjonsnummer": "123456789",
                  "type": "DuplikatKey",
                  "type": "Organisasjon"
                },
                "arbeidstaker": {
                  "type": "Person",
                  "aktoerId": 1234567890,
                  "offentligIdent": 31126700000
                },
                "type": "ordinaertArbeidsforhold"
              },
              {
                "ansettelsesperiode": {
                  "bruksperiode": {
                    "fom": "2015-08-13T10:19:33.697"
                  },
                  "periode": {
                    "fom": "2000-04-24"
                  }
                },
                "arbeidsgiver": {
                  "organisasjonsnummer": "981585216",
                  "type": "Organisasjon"
                },
                "navArbeidsforholdId": 34977713,
                "arbeidsforholdId": "1012-20000424-1",
                "arbeidstaker": {
                  "offentligIdent": "12107548740",
                  "aktoerId": "1000036350643",
                  "type": "Person"
                },
                "type": "ordinaertArbeidsforhold"
              },
             {
                "navArbeidsforholdId":53839531,
                "arbeidsforholdId":"3",
                "type":"ordinaertArbeidsforhold",
                "ansettelsesperiode":{
                   "bruksperiode":{
                      "fom":"2021-11-15T10:43:40.101"
                   },
                   "periode":{
                      "fom": "2014-07-01",
                      "tom": "2015-12-31"
                   }
                },
                "arbeidsgiver":{
                   "offentligIdent":"10047206508",
                   "aktoerId":"2142740417741",
                   "type":"Person"
                },
                "arbeidstaker":{
                   "offentligIdent":"14026223262",
                   "aktoerId":"2885922102245",
                   "type":"Person"
                }
             }
            ]
        """.trimIndent()
        }
        "14047316486" -> {
            //language=json
            return """
            [
              {
                "navArbeidsforholdId": 55481340,
                "arbeidsforholdId": "1",
                "opplysningspliktig": {
                  "type": "Organisasjon",
                  "organisasjonsnummer": "928497704"
                },
                "type": "frilanserOppdragstakerHonorarPersonerMm",
                "ansettelsesperiode": {
                  "bruksperiode": {
                    "fom": "2022-02-11T12:10:54.321"
                  },
                  "periode": {
                    "tom": "2022-02-28",
                    "fom": "2020-01-01"
                  }
                },
                "arbeidsgiver": {
                  "type": "Person",
                  "offentligIdent": "805824352"
                },
                "arbeidstaker": {
                  "offentligIdent": "14047316486",
                  "type": "Person"
                }
              },
              {
                "navArbeidsforholdId": 55481251,
                "arbeidsforholdId": "2",
                "type": "frilanserOppdragstakerHonorarPersonerMm",
                "ansettelsesperiode": {
                  "bruksperiode": {
                    "fom": "2022-02-11T12:10:54.833"
                  },
                  "periode": {
                    "tom": "2022-02-28",
                    "fom": "2020-01-01"
                  }
                },
                "arbeidsgiver": {
                  "type": "Organisasjon",
                  "organisasjonsnummer": "123456789"
                },
                "arbeidstaker": {
                  "offentligIdent": "14047316486",
                  "type": "Person"
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


