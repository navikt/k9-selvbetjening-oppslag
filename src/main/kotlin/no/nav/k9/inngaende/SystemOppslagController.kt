package no.nav.k9.inngaende

import kotlinx.coroutines.runBlocking
import no.nav.k9.config.Issuers
import no.nav.k9.config.k9ObjectMapper
import no.nav.k9.inngaende.oppslag.SystemOppslagService
import no.nav.k9.utgaende.rest.NavHeaders
import no.nav.k9.ytelseFraHeader
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.siftilgangskontroll.pdl.generated.enums.IdentGruppe
import org.json.JSONArray
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

/**
 * Kun for systemkall (azure). Se OppslagController om hvorfor runBlocking brukes uten å bytte dispatcher.
 */
@RestController
@RequiredIssuers(ProtectedWithClaims(issuer = Issuers.AZURE, claimMap = ["roles=access_as_application"]))
internal class SystemOppslagController(
    private val systemOppslagService: SystemOppslagService,
) {
    private val objectMapper = k9ObjectMapper()

    // Body leses som streng og parses her, slik at tom eller ugyldig body gir 500 som før (ikke Springs 400).
    @PostMapping("/system/hent-identer")
    fun hentIdenter(
        @RequestHeader(CorrelationIdVerifier.HEADER) correlationId: String,
        @RequestBody(required = false) body: String?,
    ): ResponseEntity<String> {
        val forespørsel = objectMapper.readValue(body ?: "", HentIdenterForespørsel::class.java)

        val resultat = runBlocking(CoroutineRequestContext(CorrelationId(correlationId))) {
            systemOppslagService.hentIdenter(
                identer = forespørsel.identer,
                identGrupper = forespørsel.identGrupper
            )
        }
        return json(JSONArray(resultat))
    }

    @PostMapping("/system/hent-barn")
    fun hentBarn(
        @RequestHeader(CorrelationIdVerifier.HEADER) correlationId: String,
        @RequestHeader(NavHeaders.XK9Ytelse, required = false) ytelseHeader: String?,
        @RequestBody(required = false) body: String?,
    ): ResponseEntity<String> {
        val ytelse = ytelseFraHeader(ytelseHeader)
        val forespørsel = objectMapper.readValue(body ?: "", HentBarnForespørsel::class.java)

        val resultat = runBlocking(CoroutineRequestContext(CorrelationId(correlationId))) {
            systemOppslagService.hentBarn(
                identer = forespørsel.identer,
                ytelse = ytelse
            )
        }
        return json(JSONArray(resultat))
    }
}

internal data class HentIdenterForespørsel(
    val identer: List<String>,
    val identGrupper: List<IdentGruppe>
)

internal data class HentBarnForespørsel(
    val identer: List<String>
)
