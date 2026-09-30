package no.nav.k9.inngaende.system

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import no.nav.k9.config.Issuers
import no.nav.k9.inngaende.CoroutineRequestContext
import no.nav.k9.inngaende.CorrelationId
import no.nav.k9.inngaende.CorrelationIdVerifier
import no.nav.k9.inngaende.json
import no.nav.k9.inngaende.oppslag.SystemOppslagService
import no.nav.k9.integrasjon.common.NavHeaders
import no.nav.k9.ytelseFraHeader
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

/**
 * Kun for systemkall (azure). Se MegController om hvorfor runBlocking brukes uten å bytte dispatcher.
 */
@RestController
@RequiredIssuers(ProtectedWithClaims(issuer = Issuers.AZURE, claimMap = ["roles=access_as_application"]))
internal class SystemBarnController(
    private val systemOppslagService: SystemOppslagService,
    private val objectMapper: ObjectMapper,
) {
    // Body leses som streng og parses her, slik at tom eller ugyldig body gir 500 som før (ikke Springs 400).
    @PostMapping("/system/hent-barn")
    fun hentBarn(
        @RequestHeader(CorrelationIdVerifier.HEADER) correlationId: String,
        @RequestHeader(NavHeaders.XK9Ytelse, required = false) ytelseHeader: String?,
        @RequestBody(required = false) body: String?,
    ): ResponseEntity<List<SystemBarnResponse>> {
        val ytelse = ytelseFraHeader(ytelseHeader)
        val forespørsel = objectMapper.readValue(body ?: "", HentBarnForespørsel::class.java)

        val resultat = runBlocking(CoroutineRequestContext(CorrelationId(correlationId))) {
            systemOppslagService.hentBarn(
                identer = forespørsel.identer,
                ytelse = ytelse
            )
        }
        return json(resultat.map { it.somResponse() })
    }
}

internal data class HentBarnForespørsel(
    val identer: List<String>
)
