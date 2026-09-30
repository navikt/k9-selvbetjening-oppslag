package no.nav.k9.inngaende.system

import no.nav.k9.config.Issuers
import no.nav.k9.inngaende.oppslag.SystemOppslagService
import no.nav.k9.integrasjon.common.NavHeaders
import no.nav.k9.ytelseFraHeader
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
@RequiredIssuers(ProtectedWithClaims(issuer = Issuers.AZURE, claimMap = ["roles=access_as_application"]))
internal class SystemBarnController(
    private val systemOppslagService: SystemOppslagService,
) {
    @PostMapping("/system/hent-barn")
    fun hentBarn(
        @RequestHeader(NavHeaders.XK9Ytelse, required = false) ytelseHeader: String?,
        @RequestBody forespørsel: HentBarnForespørsel,
    ): List<SystemBarnResponse> {
        val ytelse = ytelseFraHeader(ytelseHeader)

        val resultat = systemOppslagService.hentBarn(
            identer = forespørsel.identer,
            ytelse = ytelse
        )
        return resultat.map { it.somResponse() }
    }
}

internal data class HentBarnForespørsel(
    val identer: List<String>
)
