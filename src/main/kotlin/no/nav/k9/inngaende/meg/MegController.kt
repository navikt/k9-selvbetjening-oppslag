package no.nav.k9.inngaende.meg

import jakarta.servlet.http.HttpServletRequest
import no.nav.k9.config.Issuers
import no.nav.k9.inngaende.json
import no.nav.k9.inngaende.oppslag.INKLUDER_ALLE_ANSETTELSESPERIODER
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.inngaende.oppslag.OppslagResponse
import no.nav.k9.inngaende.oppslag.OppslagService
import no.nav.k9.inngaende.oppslag.hentAttributter
import no.nav.k9.inngaende.oppslag.hentFraOgMedTilOgMed
import no.nav.k9.inngaende.oppslag.somResponse
import no.nav.k9.inngaende.personIdent
import no.nav.k9.integrasjon.common.NavHeaders
import no.nav.k9.ytelseFraHeader
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.TOKEN_X, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
)
internal class MegController(
    private val oppslagService: OppslagService,
    private val tokenValidationContextHolder: TokenValidationContextHolder,
) {

    @GetMapping("/meg")
    fun meg(
        request: HttpServletRequest,
        @RequestHeader(NavHeaders.XK9Ytelse, required = false) ytelseHeader: String?,
    ): ResponseEntity<OppslagResponse> {
        val attributter = request.hentAttributter()
        val ytelse = ytelseFraHeader(ytelseHeader)
        if (attributter.isEmpty()) return json(OppslagResponse())

        val (fraOgMed, tilOgMed) = request.hentFraOgMedTilOgMed()
        val inkluderAlleAnsettelsesperioder = request.getParameter(INKLUDER_ALLE_ANSETTELSESPERIODER)?.toBoolean() == true
        val ident = Ident(tokenValidationContextHolder.personIdent())

        val oppslagResultat = oppslagService.oppslag(
            ident = ident,
            attributter = attributter,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed,
            inkluderAlleAnsettelsesperioder = inkluderAlleAnsettelsesperioder,
            ytelse = ytelse
        )
        return json(oppslagResultat.somResponse(attributter))
    }
}
