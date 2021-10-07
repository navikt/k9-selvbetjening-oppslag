package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import no.nav.k9.BarnFødselsnummer.BARN_TIL_PERSON_1
import no.nav.k9.BarnFødselsnummer.BARN_TIL_PERSON_2
import no.nav.k9.BarnFødselsnummer.DØD_BARN_TIL_PERSON_4
import no.nav.k9.BarnFødselsnummer.SKJERMET_BARN_TIL_PERSON_3
import no.nav.k9.PersonFødselsnummer.DØD_PERSON
import no.nav.k9.PersonFødselsnummer.PERSON_1_MED_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_2_MED_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_3_MED_SKJERMET_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_4_MED_DØD_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_UNDER_MYNDIGHETS_ALDER
import no.nav.k9.clients.pdl.generated.hentperson.Navn
import no.nav.siftilgangskontroll.pdl.generated.enums.AdressebeskyttelseGradering
import no.nav.siftilgangskontroll.pdl.generated.enums.ForelderBarnRelasjonRolle
import no.nav.siftilgangskontroll.pdl.generated.hentbarn.Folkeregisteridentifikator
import no.nav.siftilgangskontroll.pdl.generated.hentperson.Doedsfall
import no.nav.siftilgangskontroll.pdl.generated.hentperson.Foedsel
import no.nav.siftilgangskontroll.pdl.generated.hentperson.ForelderBarnRelasjon
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
        logger.info("Hentet personIdent fra request: {}", ident)

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

private fun getResponse(
    navIdent: String,
    adressebeskyttelseGraderinger: List<AdressebeskyttelseGradering> = listOf(),
): String {
    // søker
    val folkeregisteridentifikator = listOf(Folkeregisteridentifikator(navIdent))
    var fødselsdato: List<Foedsel>
    var navn: List<Navn>
    var dødsfall: List<Doedsfall> = listOf()

    // barn
    var forelderBarnRelasjon: List<ForelderBarnRelasjon> = listOf()

    when (navIdent) {
        PERSON_1_MED_BARN -> {
            navn = listOf(Navn(
                fornavn = "STOR-KAR",
                mellomnavn = "LANGEMANN",
                etternavn = "TEST",
                forkortetNavn = "STOR-KAR LANGEMANN TEST"
            ))

            fødselsdato = listOf(Foedsel("1985-07-27"))

            forelderBarnRelasjon = listOf(ForelderBarnRelasjon(
                relatertPersonsIdent = BARN_TIL_PERSON_1,
                relatertPersonsRolle = ForelderBarnRelasjonRolle.BARN,
                minRolleForPerson = ForelderBarnRelasjonRolle.MOR
            ))
        }

        PERSON_2_MED_BARN -> {
            navn = listOf(Navn(
                fornavn = "ARNE",
                mellomnavn = "BJARNE",
                etternavn = "CARLSEN",
                forkortetNavn = "ARNE BJARNE CARLSEN"
            ))

            fødselsdato = listOf(Foedsel("1990-01-02"))


            forelderBarnRelasjon = listOf(ForelderBarnRelasjon(
                relatertPersonsIdent = BARN_TIL_PERSON_2,
                relatertPersonsRolle = ForelderBarnRelasjonRolle.BARN,
                minRolleForPerson = ForelderBarnRelasjonRolle.MOR
            ))
        }

        PERSON_3_MED_SKJERMET_BARN -> {
            navn = listOf(Navn(
                fornavn = "Ole",
                mellomnavn = "Med Gradert",
                etternavn = "Barn",
                forkortetNavn = "Ole Med Gradert Barn"
            ))

            fødselsdato = listOf(Foedsel("1990-01-02"))

            forelderBarnRelasjon = listOf(ForelderBarnRelasjon(
                relatertPersonsIdent = SKJERMET_BARN_TIL_PERSON_3,
                relatertPersonsRolle = ForelderBarnRelasjonRolle.BARN,
                minRolleForPerson = ForelderBarnRelasjonRolle.FAR
            ))
        }

        PERSON_4_MED_DØD_BARN -> {
            navn = listOf(Navn(
                fornavn = "Bjarne",
                mellomnavn = "Med Død",
                etternavn = "Barn",
                forkortetNavn = "Bjarne Med Død Barn"
            ))

            fødselsdato = listOf(Foedsel("1990-01-02"))

            forelderBarnRelasjon = listOf(ForelderBarnRelasjon(
                relatertPersonsIdent = DØD_BARN_TIL_PERSON_4,
                relatertPersonsRolle = ForelderBarnRelasjonRolle.BARN,
                minRolleForPerson = ForelderBarnRelasjonRolle.FAR
            ))
        }

        DØD_PERSON -> {
            navn = listOf(Navn(
                fornavn = "Død",
                mellomnavn = "",
                etternavn = "Person",
                forkortetNavn = "Død Person"
            ))

            fødselsdato = listOf(Foedsel("1990-01-02"))

            dødsfall = listOf(Doedsfall("2021-01-01"))
        }

        PERSON_UNDER_MYNDIGHETS_ALDER -> {
            navn = listOf(Navn(
                fornavn = "Person",
                mellomnavn = "Under",
                etternavn = "Myndighetsalder",
                forkortetNavn = "Person Under Myndighetsalder"
            ))

            fødselsdato = listOf(Foedsel("2006-01-01"))
        }

        else -> {
            navn = listOf(Navn(
                fornavn = "CATO",
                mellomnavn = "",
                etternavn = "NILSEN",
                forkortetNavn = "Cato Nilsen"
            ))

            fødselsdato = listOf(Foedsel("1980-05-20"))
        }
    }

    //language=json
    return """
            {
                "data": {
                    "hentPerson": {
                        "navn": ${navn.navnSomJsonArray()},
                        "folkeregisteridentifikator": ${folkeregisteridentifikator.folkeregisteridentifikatorSomJsonArray()},
                        "adressebeskyttelse": ${adressebeskyttelseGraderinger.adressebeskyttelseGraderingJsonArray()},
                        "foedsel": ${fødselsdato.fødselSomJsonArray()},
                        "doedsfall": ${dødsfall.dødsfallSomJsonArray()},
                        "forelderBarnRelasjon": ${forelderBarnRelasjon.forelderBarnRelasjonSomJsonArray()}
                    }
                }
            }
    """.trimIndent()
}



