package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.AktoerRegisterV1Gateway
import no.nav.k9.utgaende.gateway.TpsProxyV1Gateway
import no.nav.k9.utgaende.rest.AktoerId
import no.nav.k9.utgaende.rest.TpsPerson

internal class MegOppslag(
    private val aktoerRegisterV1Gateway: AktoerRegisterV1Gateway,
    private val tpsProxyV1Gateway: TpsProxyV1Gateway
) {
    internal suspend fun meg(
        ident: Ident,
        attributter: Set<Attributt>
    ) = Meg(
        tpsPerson = tpsProxyV1Gateway.person(
            ident = ident,
            attributter = attributter
        ),
        aktoerId = aktoerRegisterV1Gateway.aktoerId(
            ident = ident,
            attributter = attributter
        )
    )
}

internal data class Meg(
    internal val tpsPerson: TpsPerson?,
    internal val aktoerId: AktoerId?
)