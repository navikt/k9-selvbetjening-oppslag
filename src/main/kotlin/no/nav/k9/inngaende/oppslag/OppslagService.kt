package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.AktoerRegisterV1Gateway
import no.nav.k9.utgaende.gateway.PersonV3Gateway
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.metadata.Endringstyper

internal class OppslagService(
    private val personV3Gateway: PersonV3Gateway,
    private val aktoerRegisterV1Gateway: AktoerRegisterV1Gateway
) {

    private companion object {
        private val aktiveEndringstyper = listOf(Endringstyper.NY, Endringstyper.ENDRET, null)
        private val personStatusDød = listOf("DØD", "DØDD")
        private const val BARN = "BARN"
    }

    internal suspend fun oppslag(
        fødselsnummer: Fødselsnummer,
        attributter: Set<Attributt>) : OppslagResultat {

        val personV3 = personV3Gateway.person(fødselsnummer, attributter)

        return OppslagResultat(
            meg = meg(
                attributter = attributter,
                fødselsnummer = fødselsnummer,
                person = personV3
            ),
            barn = barn(
                attributter = attributter,
                person = personV3
            )
        )
    }

    private suspend fun meg(
        person: Person?,
        fødselsnummer: Fødselsnummer,
        attributter: Set<Attributt>
    ) : Meg? {
        return if (!attributter.etterspurtMeg()) null else {
            Meg(
                person = person,
                aktørId = aktoerRegisterV1Gateway.aktørId(
                    fødselsnummer = fødselsnummer,
                    attributter = attributter
                )
            )
        }

    }

    private suspend fun barn(
        attributter: Set<Attributt>,
        person: Person?
    ) : Set<Barn>? {
        if (!attributter.etterspurtBarn()) return null

        val barn = person!!.harFraRolleI
            ?.filter { it.tilRolle.value == BARN }
            ?.filter { aktiveEndringstyper.contains(it.endringstype) }
            ?.filter { it.tilPerson.lever() }
            ?.map {
            Barn(
                person = it.tilPerson,
                aktørId = aktoerRegisterV1Gateway.aktørId(
                    fødselsnummer = it.tilPerson.ident(),
                    attributter = attributter
                )
            )
        }?.toSet()

        return barn ?: emptySet()
    }

    private fun Person.lever() = !personStatusDød.contains(personstatus.personstatus.value.toUpperCase())
    private fun Person.ident() = Fødselsnummer((aktoer as PersonIdent).ident.ident)
}