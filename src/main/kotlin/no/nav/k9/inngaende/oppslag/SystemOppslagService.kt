package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.PDLProxyGateway
import no.nav.siftilgangskontroll.pdl.generated.enums.IdentGruppe
import no.nav.siftilgangskontroll.pdl.generated.hentidenterbolk.HentIdenterBolkResult

class SystemOppslagService(
    private val pdlProxyGateway: PDLProxyGateway
) {

    suspend fun hentIdenter(identer: List<String>, identGrupper: List<IdentGruppe>): List<HentIdenterBolkResult> {
        return pdlProxyGateway.hentIdenter(identer, identGrupper)
    }
}
