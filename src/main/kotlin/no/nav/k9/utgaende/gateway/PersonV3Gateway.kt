package no.nav.k9.utgaende.gateway

import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Fødselsnummer
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.*
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest

internal class PersonV3Gateway (private val personV3: PersonV3) {

    internal companion object {
        private val personAttributter = setOf(
            Attributt.fornavn,
            Attributt.mellomnavn,
            Attributt.etternavn,
            Attributt.fødselsdato,
            Attributt.barnFornavn,
            Attributt.barnMellomnavn,
            Attributt.barnEtternavn,
            Attributt.barnFødselsdato
        )

        private val kreverFamlierelasjoner = setOf(
            Attributt.barnFornavn,
            Attributt.barnMellomnavn,
            Attributt.barnEtternavn,
            Attributt.barnFødselsdato
        )
    }

    internal fun person(
        fødselsnummer: Fødselsnummer,
        attributter: Set<Attributt>) : Person? {

        val aktuelleAttributter = attributter.toMutableSet()
        aktuelleAttributter.removeIf { !personAttributter.contains(it) }

        if (aktuelleAttributter.isEmpty()) return null

        val request = HentPersonRequest()

        request.aktoer = PersonIdent().apply {
            ident = NorskIdent().apply { ident = fødselsnummer.value }
        }

        val informasjonsbehov = aktuelleAttributter.tilInformasjonsbehov()

        if (informasjonsbehov.isNotEmpty()) {
            request.withInformasjonsbehov(informasjonsbehov)
        }

        return personV3.hentPerson(request).person
    }

    private fun Set<Attributt>.tilInformasjonsbehov() : Set<Informasjonsbehov> {
        val informasjonsbehov = mutableSetOf<Informasjonsbehov>()
        if (any { it in kreverFamlierelasjoner }) informasjonsbehov.add(Informasjonsbehov.FAMILIERELASJONER)
        return informasjonsbehov.toSet()
    }
}