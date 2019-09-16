package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.AktoerRegisterV1Gateway
import no.nav.k9.utgaende.gateway.PersonV3Gateway
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.metadata.Endringstyper

internal class BarnOppslag(
    private val aktoerRegisterV1Gateway: AktoerRegisterV1Gateway,
    private val personV3Gateway: PersonV3Gateway
) {
    private companion object {
        private val aktiveEndringstyper = listOf(Endringstyper.NY, Endringstyper.ENDRET, null)
        private val personStatusDød = listOf("DØD", "DØDD")
        private const val BARN = "BARN"

        private  val barnAttributter = setOf(
            Attributt.fornavn,
            Attributt.mellomnavn,
            Attributt.etternavn,
            Attributt.fødselsdato,
            Attributt.status,
            Attributt.diskresjonskode
        )
    }


    internal suspend fun barn(
        attributter: Set<Attributt>,
        person: Person?
    ) : Set<Barn>? {
        if (!attributter.etterspurtBarn()) return null

        val barn = person!!.harFraRolleI
            ?.filter { it.tilRolle.value == BARN }
            ?.filter { aktiveEndringstyper.contains(it.endringstype) }
            ?.mapNotNull {
                personV3Gateway.person(
                    fødselsnummer = it.tilPerson.ident(),
                    attributter = barnAttributter
                )
            }
            ?.filter { it.lever() }
            ?.map {
                Barn(
                    person = it,
                    aktørId = aktoerRegisterV1Gateway.aktørId(
                        fødselsnummer = person.ident(),
                        attributter = attributter
                    )
                )
            }?.toSet()

        return barn ?: emptySet()
    }
    private fun Person.lever() = !personStatusDød.contains(personstatus.personstatus.value.toUpperCase())
    private fun Person.ident() = Fødselsnummer((aktoer as PersonIdent).ident.ident)
}