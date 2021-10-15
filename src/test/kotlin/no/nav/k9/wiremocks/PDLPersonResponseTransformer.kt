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
import no.nav.siftilgangskontroll.core.pdl.utils.*
import no.nav.siftilgangskontroll.pdl.generated.enums.ForelderBarnRelasjonRolle
import no.nav.siftilgangskontroll.pdl.generated.hentperson.*
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
    navIdent: String
): String {
    val person: Person = when (navIdent) {
        PERSON_1_MED_BARN -> {
            Person(
                folkeregisteridentifikator = listOf(Folkeregisteridentifikator(navIdent)),
                navn = listOf(Navn(
                    fornavn = "STOR-KAR",
                    mellomnavn = "LANGEMANN",
                    etternavn = "TEST",
                    forkortetNavn = "STOR-KAR LANGEMANN TEST"
                )),
                foedsel = listOf(Foedsel("1985-07-27")),
                adressebeskyttelse = listOf(),
                doedsfall = listOf(),
                forelderBarnRelasjon = listOf(ForelderBarnRelasjon(
                    relatertPersonsIdent = BARN_TIL_PERSON_1,
                    relatertPersonsRolle = ForelderBarnRelasjonRolle.BARN,
                    minRolleForPerson = ForelderBarnRelasjonRolle.MOR
                ))
            )
        }

        PERSON_2_MED_BARN -> {
            Person(
                folkeregisteridentifikator = listOf(Folkeregisteridentifikator(navIdent)),
                navn = listOf(Navn(
                    fornavn = "ARNE",
                    mellomnavn = "BJARNE",
                    etternavn = "CARLSEN",
                    forkortetNavn = "ARNE BJARNE CARLSEN"
                )),
                foedsel = listOf(Foedsel("1990-01-02")),
                adressebeskyttelse = listOf(),
                doedsfall = listOf(),
                forelderBarnRelasjon = listOf(ForelderBarnRelasjon(
                    relatertPersonsIdent = BARN_TIL_PERSON_2,
                    relatertPersonsRolle = ForelderBarnRelasjonRolle.BARN,
                    minRolleForPerson = ForelderBarnRelasjonRolle.MOR
                ))
            )
        }

        PERSON_3_MED_SKJERMET_BARN -> {
            Person(
                folkeregisteridentifikator = listOf(Folkeregisteridentifikator(navIdent)),
                navn = listOf(Navn(
                    fornavn = "Ole",
                    mellomnavn = "Med Gradert",
                    etternavn = "Barn",
                    forkortetNavn = "Ole Med Gradert Barn"
                )),
                foedsel = listOf(Foedsel("1990-01-02")),
                adressebeskyttelse = listOf(),
                doedsfall = listOf(),
                forelderBarnRelasjon = listOf(ForelderBarnRelasjon(
                    relatertPersonsIdent = SKJERMET_BARN_TIL_PERSON_3,
                    relatertPersonsRolle = ForelderBarnRelasjonRolle.BARN,
                    minRolleForPerson = ForelderBarnRelasjonRolle.FAR
                ))
            )
        }

        PERSON_4_MED_DØD_BARN -> {
            Person(
                folkeregisteridentifikator = listOf(Folkeregisteridentifikator(navIdent)),
                navn = listOf(Navn(
                    fornavn = "Bjarne",
                    mellomnavn = "Med Død",
                    etternavn = "Barn",
                    forkortetNavn = "Bjarne Med Død Barn"
                )),
                foedsel = listOf(Foedsel("1990-01-02")),
                adressebeskyttelse = listOf(),
                doedsfall = listOf(),
                forelderBarnRelasjon = listOf(ForelderBarnRelasjon(
                    relatertPersonsIdent = DØD_BARN_TIL_PERSON_4,
                    relatertPersonsRolle = ForelderBarnRelasjonRolle.BARN,
                    minRolleForPerson = ForelderBarnRelasjonRolle.FAR
                ))
            )
        }

        DØD_PERSON -> {
            Person(
                folkeregisteridentifikator = listOf(Folkeregisteridentifikator(navIdent)),
                navn = listOf(Navn(
                    fornavn = "Død",
                    mellomnavn = "",
                    etternavn = "Person",
                    forkortetNavn = "Død Person"
                )),
                foedsel = listOf(Foedsel("1990-01-02")),
                adressebeskyttelse = listOf(),
                doedsfall = listOf(Doedsfall("2021-01-01")),
                forelderBarnRelasjon = listOf())
        }

        PERSON_UNDER_MYNDIGHETS_ALDER -> {
            Person(
                folkeregisteridentifikator = listOf(Folkeregisteridentifikator(navIdent)),
                navn = listOf(Navn(
                    fornavn = "Person",
                    mellomnavn = "Under",
                    etternavn = "Myndighetsalder",
                    forkortetNavn = "Person Under Myndighetsalder"
                )),
                foedsel = listOf(Foedsel("2006-01-01")),
                adressebeskyttelse = listOf(),
                doedsfall = listOf(),
                forelderBarnRelasjon = listOf(),
            )
        }

        else -> {
            Person(
                folkeregisteridentifikator = listOf(Folkeregisteridentifikator(navIdent)),
                navn = listOf(Navn(
                    fornavn = "CATO",
                    mellomnavn = "",
                    etternavn = "NILSEN",
                    forkortetNavn = "Cato Nilsen"
                )),
                foedsel = listOf(Foedsel("1980-05-20")),
                adressebeskyttelse = listOf(),
                doedsfall = listOf(),
                forelderBarnRelasjon = listOf(),
            )
        }
    }

    return pdlHentPersonResponse(person)
}



