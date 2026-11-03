package no.nav.k9.inngaende.system

import no.nav.k9.config.Issuers
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.inngaende.oppslag.SystemOppslagService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * Systemoppslag av arbeidsgivere for en person, uten innlogget borger.
 * Første konsument er k9-sak-innsyn-api, som vurderer om søkeren kan bruke endringsmeldingen
 * på vegne av en saksbehandler. Tilgangskontrollen for saksbehandleren skjer i k9-sak, ikke her.
 *
 * Ingen X-K9-Ytelse-header, i motsetning til hent-barn: aareg-oppslaget avhenger ikke av ytelse.
 */
@RestController
@RequiredIssuers(ProtectedWithClaims(issuer = Issuers.AZURE, claimMap = ["roles=access_as_application"]))
internal class SystemArbeidsgivereController(
    private val systemOppslagService: SystemOppslagService,
) {
    @PostMapping("/system/hent-arbeidsgivere")
    fun hentArbeidsgivere(
        @RequestBody forespørsel: HentArbeidsgivereForespørsel,
    ): SystemArbeidsgivereResponse {
        val arbeidsgivere = systemOppslagService.hentArbeidsgivere(
            ident = Ident(forespørsel.ident),
            fraOgMed = forespørsel.fraOgMed,
            tilOgMed = forespørsel.tilOgMed,
            inkluderAlleAnsettelsesperioder = forespørsel.inkluderAlleAnsettelsesperioder,
        )
        return arbeidsgivere.somSystemResponse()
    }
}

internal data class HentArbeidsgivereForespørsel(
    val ident: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val inkluderAlleAnsettelsesperioder: Boolean = false,
)
