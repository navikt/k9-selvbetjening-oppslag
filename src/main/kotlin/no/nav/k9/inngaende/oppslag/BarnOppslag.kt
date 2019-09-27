package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.AktoerRegisterV1Gateway
import no.nav.k9.utgaende.gateway.TpsProxyV1Gateway
import no.nav.k9.utgaende.rest.AktoerId
import no.nav.k9.utgaende.rest.TpsBarn

internal class BarnOppslag(
    private val aktoerRegisterV1Gateway: AktoerRegisterV1Gateway,
    private val tpsProxyV1Gateway: TpsProxyV1Gateway
) {

    internal suspend fun barn(
        ident: Ident,
        attributter: Set<Attributt>
    ) : Set<Barn>? {
        if (!attributter.etterspurtBarn()) return null

        val tpsBarn = tpsProxyV1Gateway.barn(
            ident = ident,
            attributter = attributter
        ) ?: return null

        return tpsBarn
            .filter { it.doedsdato == null }
            .map {
                Barn(
                    tpsBarn = it,
                    aktoerId = aktoerRegisterV1Gateway.aktoerId(
                        ident = ident,
                        attributter = attributter
                    )
                )
            }.toSet()
    }
}

internal data class Barn(
    internal val tpsBarn: TpsBarn?,
    internal val aktoerId: AktoerId?
)