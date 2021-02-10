package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.AktoerRegisterV1Gateway
import no.nav.k9.utgaende.gateway.PDLProxyGateway
import no.nav.k9.utgaende.gateway.TpsProxyV1Gateway
import no.nav.k9.utgaende.rest.AktørId
import no.nav.k9.utgaende.rest.PersonPdl
import no.nav.k9.utgaende.rest.TpsPerson

internal class Personppslag(
    private val aktoerRegisterV1Gateway: AktoerRegisterV1Gateway,
    private val pdlProxyGateway: PDLProxyGateway
) {
    internal suspend fun person(
        ident: Ident,
        attributter: Set<Attributt>
    ) = Person(
        pdlPerson = pdlProxyGateway.person(ident = ident).person,
        aktørId = aktoerRegisterV1Gateway.aktørId(
            ident = ident,
            attributter = attributter
        )
    )
}

internal data class Person(
    internal val pdlPerson: PersonPdl?,
    internal val aktørId: AktørId?
)
