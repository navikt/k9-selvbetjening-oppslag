package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.AktoerRegisterV1Gateway
import no.nav.k9.utgaende.gateway.PersonV3Gateway
import no.nav.k9.utgaende.rest.AktørId
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
        private val kode6kode7 = listOf("SPSF", "SPFO")
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
                it.tilPerson
//                personV3Gateway.person(
//                    ident = it.tilPerson.ident(),
//                    attributter = barnAttributter
//                )
            }
            ?.filter { it.lever() }
            ?.filter { it.ikkeKode6ellerKode7() }
            ?.map {
                Barn(
                    person = it,
                    aktørId = aktoerRegisterV1Gateway.aktørId(
                        ident = person.ident(),
                        attributter = attributter
                    )
                )
            }?.toSet()

        return barn ?: emptySet()
    }
    private fun Person.lever() = doedsdato == null
    private fun Person.ikkeKode6ellerKode7() = !kode6kode7.contains(diskresjonskode?.value)
    private fun Person.ident() = Ident((aktoer as PersonIdent).ident.ident)
}

internal data class Barn(
    internal val person: Person?,
    internal val aktørId: AktørId?
)