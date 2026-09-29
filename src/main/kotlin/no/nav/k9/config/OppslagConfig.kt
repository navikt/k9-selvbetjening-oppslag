package no.nav.k9.config

import no.nav.k9.inngaende.oppslag.ArbeidsgivereOppslag
import no.nav.k9.inngaende.oppslag.BarnOppslag
import no.nav.k9.inngaende.oppslag.MegOppslag
import no.nav.k9.inngaende.oppslag.OppslagService
import no.nav.k9.inngaende.oppslag.SystemOppslagService
import no.nav.k9.utgaende.auth.PdlAuthService
import no.nav.k9.utgaende.gateway.ArbeidsgiverOgArbeidstakerRegisterGateway
import no.nav.k9.utgaende.gateway.EnhetsregisterV1Gateway
import no.nav.k9.utgaende.gateway.PDLProxyGateway
import no.nav.siftilgangskontroll.core.tilgang.TilgangService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class OppslagConfig {

    @Bean("pdlProxyGateway")
    fun pdlProxyGateway(tilgangService: TilgangService, pdlAuthService: PdlAuthService) =
        PDLProxyGateway(tilgangService = tilgangService, pdlAuthService = pdlAuthService)

    @Bean("oppslagService")
    internal fun oppslagService(
        pdlProxyGateway: PDLProxyGateway,
        enhetsregisterV1Gateway: EnhetsregisterV1Gateway,
        arbeidsgiverOgArbeidstakerRegisterGateway: ArbeidsgiverOgArbeidstakerRegisterGateway,
    ) = OppslagService(
        megOppslag = MegOppslag(pdlProxyGateway),
        barnOppslag = BarnOppslag(pdlProxyGateway),
        arbeidsgiverOppslag = ArbeidsgivereOppslag(enhetsregisterV1Gateway = enhetsregisterV1Gateway),
        arbeidsgiverOgArbeidstakerRegisterGateway = arbeidsgiverOgArbeidstakerRegisterGateway
    )

    @Bean("systemOppslagService")
    internal fun systemOppslagService(pdlProxyGateway: PDLProxyGateway) =
        SystemOppslagService(pdlProxyGateway = pdlProxyGateway)
}
