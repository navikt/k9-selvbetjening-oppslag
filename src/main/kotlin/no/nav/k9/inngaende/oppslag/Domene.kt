package no.nav.k9.inngaende.oppslag

import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person

internal data class Fødselsnummer(internal val value: String)
internal enum class Attributt {fornavn, mellomnavn, etternavn}
internal data class OppslagResultat(internal val personV3: Person?)