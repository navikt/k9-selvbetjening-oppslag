package no.nav.k9.inngaende.system

import no.nav.k9.Ytelse
import no.nav.k9.integrasjon.common.NavHeaders
import no.nav.k9.ytelseFraHeader
import no.nav.k9.config.Issuers
import no.nav.k9.inngaende.oppslag.SystemOppslagService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.siftilgangskontroll.pdl.generated.enums.IdentGruppe
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
@RequiredIssuers(ProtectedWithClaims(issuer = Issuers.AZURE, claimMap = ["roles=access_as_application"]))
internal class SystemIdenterController(
    private val systemOppslagService: SystemOppslagService,
) {
    @PostMapping("/system/hent-identer")
    fun hentIdenter(
        @RequestHeader(NavHeaders.XK9Ytelse, required = false) ytelseHeader: String?,
        @RequestBody forespørsel: HentIdenterForespørsel,
    ): List<IdenterBolkResponse> {
        // TODO: gjør headeren påkrevd når alle konsumenter sender den.
        val ytelse = ytelseHeader?.let { ytelseFraHeader(it) } ?: Ytelse.PLEIEPENGER_SYKT_BARN // Fallback for konsumenter som ikke sender ytelse
        val resultat = systemOppslagService.hentIdenter(
            identer = forespørsel.identer,
            identGrupper = forespørsel.identGrupper,
            ytelse = ytelse
        )
        return resultat.map { it.somResponse() }
    }
}

internal data class HentIdenterForespørsel(
    val identer: List<String>,
    val identGrupper: List<IdentGruppe>
)
