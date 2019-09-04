package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.PersonV3Gateway

internal class OppslagService(
    private val personV3Gateway: PersonV3Gateway
) {

    internal fun oppslag(
        fødselsnummer: Fødselsnummer,
        attributter: Set<Attributt>) = OppslagResultat(
        personV3 = personV3Gateway.person(fødselsnummer, attributter)
    )
}