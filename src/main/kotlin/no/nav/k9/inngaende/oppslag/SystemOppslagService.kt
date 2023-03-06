package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.PDLProxyGateway
import no.nav.siftilgangskontroll.pdl.generated.enums.IdentGruppe
import no.nav.siftilgangskontroll.pdl.generated.hentidenterbolk.HentIdenterBolkResult
import org.slf4j.LoggerFactory

class SystemOppslagService(
    private val pdlProxyGateway: PDLProxyGateway
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(SystemOppslagService::class.java)
    }

    suspend fun hentIdenter(identer: List<String>, identGrupper: List<IdentGruppe>): List<HentIdenterBolkResult> {
        logger.info("Henter identer med systemkall.")
        return pdlProxyGateway.hentIdenter(identer, identGrupper)
    }
}
