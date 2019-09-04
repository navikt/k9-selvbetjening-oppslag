package no.nav.k9.utgaende.gateway

import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Fødselsnummer
import no.nav.tjeneste.virksomhet.person.v3.binding.PersonV3
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonRequest

internal class PersonV3Gateway (private val personV3: PersonV3) {

    internal companion object {
        internal val støttedeAttributter = setOf(
            Attributt.fornavn,
            Attributt.mellomnavn,
            Attributt.etternavn
        )
    }

    internal fun person(
        fødselsnummer: Fødselsnummer,
        attributter: Set<Attributt>) : Person? {

        val brukteAttrbutter = attributter.toMutableSet()
        brukteAttrbutter.removeIf { !støttedeAttributter.contains(it) }

        if (brukteAttrbutter.isEmpty()) return null

        val request = HentPersonRequest()

        request.aktoer = PersonIdent().apply {
            ident = NorskIdent().apply { ident = fødselsnummer.value }
        }

        val informasjonsbehov = brukteAttrbutter.tilInformasjonsbehov()

        if (informasjonsbehov.isNotEmpty()) {
            request.withInformasjonsbehov(informasjonsbehov)
        }

        return personV3.hentPerson(request).person
    }

    private fun Set<Attributt>.tilInformasjonsbehov() : Set<Informasjonsbehov> {
        return setOf()
    }
}