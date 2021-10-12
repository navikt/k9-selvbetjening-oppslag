package no.nav.k9.wiremocks

import no.nav.k9.clients.pdl.generated.hentperson.Navn
import no.nav.siftilgangskontroll.pdl.generated.enums.AdressebeskyttelseGradering
import no.nav.siftilgangskontroll.pdl.generated.hentbarn.Folkeregisteridentifikator
import no.nav.siftilgangskontroll.pdl.generated.hentperson.Doedsfall
import no.nav.siftilgangskontroll.pdl.generated.hentperson.Foedsel
import no.nav.siftilgangskontroll.pdl.generated.hentperson.ForelderBarnRelasjon
import org.json.JSONArray
import org.json.JSONObject

fun List<Navn>.navnSomJsonArray() = JSONArray(
    map {
        JSONObject(mapOf(
            "fornavn" to it.fornavn,
            "mellomnavn" to it.mellomnavn,
            "etternavn" to it.etternavn,
            "forkortetNavn" to it.forkortetNavn,
        ))
    }
)

fun List<AdressebeskyttelseGradering>.adressebeskyttelseGraderingJsonArray() = JSONArray(
    map {
        JSONObject(mapOf(
            "gradering" to it.name
        ))
    }
)

fun List<Folkeregisteridentifikator>.folkeregisteridentifikatorSomJsonArray() = JSONArray(
    map {
        JSONObject(mapOf(
            "identifikasjonsnummer" to it.identifikasjonsnummer
        ))
    }
)

fun List<ForelderBarnRelasjon>.forelderBarnRelasjonSomJsonArray() = JSONArray(
    map {
        JSONObject(mapOf(
            "relatertPersonsIdent" to it.relatertPersonsIdent,
            "relatertPersonsRolle" to it.relatertPersonsRolle,
            "minRolleForPerson" to it.minRolleForPerson,
        ))
    }
)

fun List<Doedsfall>.dødsfallSomJsonArray() = JSONArray(
    map {
        JSONObject(mapOf(
            "doedsdato" to it.doedsdato
        ))
    }
)

fun List<Foedsel>.fødselSomJsonArray() = JSONArray(
    map {
        JSONObject(mapOf(
            "foedselsdato" to it.foedselsdato
        ))
    }
)
