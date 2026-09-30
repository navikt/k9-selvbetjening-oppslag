package no.nav.k9.inngaende.system

import no.nav.k9.config.Issuers
import no.nav.k9.inngaende.oppslag.SystemOppslagService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.siftilgangskontroll.pdl.generated.enums.IdentGruppe
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

// Kun for systemkall (azure).
@RestController
@RequiredIssuers(ProtectedWithClaims(issuer = Issuers.AZURE, claimMap = ["roles=access_as_application"]))
internal class SystemIdenterController(
    private val systemOppslagService: SystemOppslagService,
) {
    @PostMapping("/system/hent-identer")
    fun hentIdenter(
        @RequestBody forespørsel: HentIdenterForespørsel,
    ): List<IdenterBolkResponse> {
        val resultat = systemOppslagService.hentIdenter(
            identer = forespørsel.identer,
            identGrupper = forespørsel.identGrupper
        )
        return resultat.map { it.somResponse() }
    }
}

internal data class HentIdenterForespørsel(
    val identer: List<String>,
    val identGrupper: List<IdentGruppe>
)
