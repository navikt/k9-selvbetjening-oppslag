package no.nav.k9.utgaende.gateway

import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Ident
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
        Ident: Ident,
        attributter: Set<Attributt>) : Person? {

        if (!attributter.any { it in personAttributter }) return null

        val request = HentPersonRequest()

        request.aktoer = PersonIdent().apply {
            ident = NorskIdent().apply { ident = Ident.value }
        }

        val informasjonsbehov = attributter.tilInformasjonsbehov()

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