package no.nav.k9.inngaende.oppslag

import no.nav.k9.Ytelse
import no.nav.k9.utgaende.gateway.PDLProxyGateway
import no.nav.siftilgangskontroll.core.tilgang.BarnResponse
import no.nav.siftilgangskontroll.pdl.generated.enums.IdentGruppe
import no.nav.siftilgangskontroll.pdl.generated.hentidenterbolk.HentIdenterBolkResult
import org.slf4j.LoggerFactory

class SystemOppslagService(
    private val pdlProxyGateway: PDLProxyGateway,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(SystemOppslagService::class.java)
    }

    suspend fun hentIdenter(identer: List<String>, identGrupper: List<IdentGruppe>): List<HentIdenterBolkResult> {
        logger.info("Henter identer med systemkall.")
        return pdlProxyGateway.hentIdenter(identer, identGrupper)
    }

    suspend fun hentBarn(identer: List<String>, ytelse: Ytelse): List<Barn> {
        logger.info("Henter barn med systemkall.")
        return pdlProxyGateway.hentBarn(identer, ytelse).map { br: BarnResponse ->
            val aktørId = pdlProxyGateway.aktørId(
                ident = Ident(br.ident),
                attributter = setOf(Attributt.barnAktørId)
            )
            Barn(
                pdlBarn = br.barn.tilPdlBarn(),
                aktørId = aktørId?.let { Ident(it.value) }
            )
        }
    }
}
