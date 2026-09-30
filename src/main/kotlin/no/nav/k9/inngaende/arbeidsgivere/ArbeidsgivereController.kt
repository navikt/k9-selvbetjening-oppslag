package no.nav.k9.inngaende.arbeidsgivere

import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.runBlocking
import no.nav.k9.config.Issuers
import no.nav.k9.inngaende.json
import no.nav.k9.inngaende.oppslag.OppslagResponse
import no.nav.k9.inngaende.oppslag.OppslagService
import no.nav.k9.inngaende.oppslag.hentAttributter
import no.nav.k9.inngaende.oppslag.hentOrganisasjoner
import no.nav.k9.inngaende.oppslag.somResponse
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

/**
 * Se MegController om hvorfor runBlocking brukes uten å bytte dispatcher.
 */
@RestController
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.TOKEN_X, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
)
internal class ArbeidsgivereController(
    private val oppslagService: OppslagService,
) {

    // TODO: Fjern når det er bekreftet at ingen konsumenter bruker /arbeidsgivere (fant ingen ved migreringen til Spring Boot).
    @GetMapping("/arbeidsgivere")
    fun arbeidsgivere(
        request: HttpServletRequest,
    ): ResponseEntity<OppslagResponse> {
        val attributter = request.hentAttributter()
        if (attributter.isEmpty()) return json(OppslagResponse())

        val organisasjoner = request.hentOrganisasjoner()
        val oppslagResultat = runBlocking {
            oppslagService.arbeidsgivere(
                attributter = attributter,
                organisasjoner = organisasjoner
            )
        }
        return json(oppslagResultat.somResponse(attributter))
    }
}
