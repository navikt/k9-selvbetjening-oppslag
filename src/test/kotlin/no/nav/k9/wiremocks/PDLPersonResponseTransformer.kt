package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.extension.ResponseTransformerV2
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.ServeEvent
import no.nav.k9.BarnFødselsnummer.BARN_MED_UKJENT_RELASJON
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
import no.nav.siftilgangskontroll.core.pdl.utils.pdlHentPersonResponse
import no.nav.siftilgangskontroll.pdl.generated.enums.ForelderBarnRelasjonRolle
import no.nav.siftilgangskontroll.pdl.generated.hentperson.*
import org.json.JSONObject
import org.slf4j.LoggerFactory

class PDLPersonResponseTransformer : ResponseTransformerV2 {
    private companion object {
        val logger = LoggerFactory.getLogger(PDLPersonResponseTransformer::class.java)
    }

    override fun getName(): String {
        return "pdl-hent-person"
    }

    override fun transform(response: Response, serveEvent: ServeEvent): Response {
        val requestBody = JSONObject(serveEvent.request.body.decodeToString())
        val ident = requestBody.getJSONObject("variables").getString("ident")
        logger.info("Hentet personIdent fra request: {}", ident)

        return Response.Builder.like(response)
            .body(getResponse(ident))
            .build()
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
                    etternavn = "TEST"
                )),
                foedselsdato = listOf(Foedselsdato(foedselsdato = "1985-07-27", foedselsaar = 1985)),
                adressebeskyttelse = listOf(),
                doedsfall = listOf(),
                forelderBarnRelasjon = listOf(ForelderBarnRelasjon(
                    relatertPersonsIdent = BARN_TIL_PERSON_1,
                    relatertPersonsRolle = ForelderBarnRelasjonRolle.BARN,
                    minRolleForPerson = ForelderBarnRelasjonRolle.MOR
                ))
            )
        }

        BARN_TIL_PERSON_1 -> {
            Person(
                folkeregisteridentifikator = listOf(Folkeregisteridentifikator(navIdent)),
                navn = listOf(Navn(
                    fornavn = "STOR-KAR",
                    mellomnavn = "LANGEMANN",
                    etternavn = "TEST"
                )),
                foedselsdato = listOf(Foedselsdato(foedselsdato = "1985-07-27", foedselsaar = 1985)),
                adressebeskyttelse = listOf(),
                doedsfall = listOf(),
                forelderBarnRelasjon = listOf(ForelderBarnRelasjon(
                    relatertPersonsIdent = PERSON_1_MED_BARN,
                    relatertPersonsRolle = ForelderBarnRelasjonRolle.FAR,
                    minRolleForPerson = ForelderBarnRelasjonRolle.BARN
                ))
            )
        }

        BARN_MED_UKJENT_RELASJON -> {
            Person(
                folkeregisteridentifikator = listOf(Folkeregisteridentifikator(navIdent)),
                navn = listOf(Navn(
                    fornavn = "STOR-KAR",
                    mellomnavn = "LANGEMANN",
                    etternavn = "TEST"
                )),
                foedselsdato = listOf(Foedselsdato(foedselsdato = "1985-07-27", foedselsaar = 1985)),
                adressebeskyttelse = listOf(),
                doedsfall = listOf(),
                forelderBarnRelasjon = listOf(ForelderBarnRelasjon(
                    relatertPersonsIdent = "07894097862",
                    relatertPersonsRolle = ForelderBarnRelasjonRolle.FAR,
                    minRolleForPerson = ForelderBarnRelasjonRolle.BARN
                ))
            )
        }

        PERSON_2_MED_BARN -> {
            Person(
                folkeregisteridentifikator = listOf(Folkeregisteridentifikator(navIdent)),
                navn = listOf(Navn(
                    fornavn = "ARNE",
                    mellomnavn = "BJARNE",
                    etternavn = "CARLSEN"
                )),
                foedselsdato = listOf(Foedselsdato(foedselsdato = "1990-01-02", foedselsaar = 1990)),
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
                    etternavn = "Barn"
                )),
                foedselsdato = listOf(Foedselsdato(foedselsdato = "1990-01-02", foedselsaar = 1990)),
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
                    etternavn = "Barn"
                )),
                foedselsdato = listOf(Foedselsdato(foedselsdato = "1990-01-02", foedselsaar = 1990)),
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
                    etternavn = "Person"
                )),
                foedselsdato = listOf(Foedselsdato(foedselsdato = "1990-01-02", foedselsaar = 1990)),
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
                    etternavn = "Myndighetsalder"
                )),
                foedselsdato = listOf(Foedselsdato(foedselsdato = "2019-01-01", foedselsaar = 2019)),
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
                    etternavn = "NILSEN"
                )),
                foedselsdato = listOf(Foedselsdato(foedselsdato = "1980-05-20", foedselsaar = 1980)),
                adressebeskyttelse = listOf(),
                doedsfall = listOf(),
                forelderBarnRelasjon = listOf(),
            )
        }
    }

    return pdlHentPersonResponse(person)
}



