package no.nav.k9.config

import no.nav.k9.inngaende.oppslag.ArbeidsgivereOppslag
import no.nav.k9.inngaende.oppslag.BarnOppslag
import no.nav.k9.inngaende.oppslag.MegOppslag
import no.nav.k9.inngaende.oppslag.OppslagService
import no.nav.k9.inngaende.oppslag.SystemOppslagService
import no.nav.k9.integrasjon.aareg.AaregService
import no.nav.k9.integrasjon.enhetsregister.EnhetsregisterService
import no.nav.k9.integrasjon.pdl.PdlProxyService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class OppslagConfig {

    @Bean("oppslagService")
    internal fun oppslagService(
        pdlProxyService: PdlProxyService,
        enhetsregisterService: EnhetsregisterService,
        aaregService: AaregService,
    ) = OppslagService(
        megOppslag = MegOppslag(pdlProxyService),
        barnOppslag = BarnOppslag(pdlProxyService),
        arbeidsgiverOppslag = ArbeidsgivereOppslag(enhetsregisterService = enhetsregisterService),
        aaregService = aaregService
    )

    @Bean("systemOppslagService")
    internal fun systemOppslagService(pdlProxyService: PdlProxyService) =
        SystemOppslagService(pdlProxyService = pdlProxyService)
}
