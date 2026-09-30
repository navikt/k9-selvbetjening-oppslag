package no.nav.k9.inngaende

import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.runBlocking
import no.nav.k9.config.Issuers
import no.nav.k9.http.ParameterType
import no.nav.k9.http.ValidationErrorResponseException
import no.nav.k9.http.Violation
import no.nav.k9.inngaende.oppslag.*
import no.nav.k9.integrasjon.common.NavHeaders
import no.nav.k9.ytelseFraHeader
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneId

private const val ATTRIBUTT_QUERY_NAVN = "a"
private const val FRA_OG_MED_QUERY_NAVN = "fom"
private const val TIL_OG_MED_QUERY_NAVN = "tom"
private const val INKLUDER_ALLE_ANSETTELSESPERIODER = "inkluderAlleAnsettelsesperioder"
private const val ORGANISASJONER = "org"

internal val jsonUtf8: MediaType = MediaType("application", "json", Charsets.UTF_8)

/**
 * Controllerne kjører på request-tråden, og domenet kalles med runBlocking uten å bytte dispatcher.
 * Tokenkonteksten til token-support ligger på tråden, og token-client-spring trenger den for token exchange.
 */
@RestController
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.TOKEN_X, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
)
internal class OppslagController(
    private val oppslagService: OppslagService,
    private val tokenValidationContextHolder: TokenValidationContextHolder,
) {
    private val logger: Logger = LoggerFactory.getLogger(OppslagController::class.java)

    @GetMapping("/meg")
    fun meg(
        request: HttpServletRequest,
        @RequestHeader(CorrelationIdVerifier.HEADER) correlationId: String,
        @RequestHeader(NavHeaders.XK9Ytelse, required = false) ytelseHeader: String?,
    ): ResponseEntity<OppslagResponse> {
        val attributter = request.hentAttributter()
        val ytelse = ytelseFraHeader(ytelseHeader)
        if (attributter.isEmpty()) return json(OppslagResponse())

        val (fraOgMed, tilOgMed) = request.hentFraOgMedTilOgMed()
        val inkluderAlleAnsettelsesperioder = request.getParameter(INKLUDER_ALLE_ANSETTELSESPERIODER)?.toBoolean() == true
        val ident = Ident(tokenValidationContextHolder.personIdent())

        val oppslagResultat = runBlocking(CoroutineRequestContext(CorrelationId(correlationId))) {
            oppslagService.oppslag(
                ident = ident,
                attributter = attributter,
                fraOgMed = fraOgMed,
                tilOgMed = tilOgMed,
                inkluderAlleAnsettelsesperioder = inkluderAlleAnsettelsesperioder,
                ytelse = ytelse
            )
        }
        return json(oppslagResultat.somResponse(attributter))
    }

    // TODO: Fjern når det er bekreftet at ingen konsumenter bruker /arbeidsgivere (fant ingen ved migreringen til Spring Boot).
    @GetMapping("/arbeidsgivere")
    fun arbeidsgivere(
        request: HttpServletRequest,
        @RequestHeader(CorrelationIdVerifier.HEADER) correlationId: String,
    ): ResponseEntity<OppslagResponse> {
        val attributter = request.hentAttributter()
        if (attributter.isEmpty()) return json(OppslagResponse())

        val organisasjoner = request.hentOrganisasjoner()
        val oppslagResultat = runBlocking(CoroutineRequestContext(CorrelationId(correlationId))) {
            oppslagService.arbeidsgivere(
                attributter = attributter,
                organisasjoner = organisasjoner
            )
        }
        return json(oppslagResultat.somResponse(attributter))
    }

    private fun HttpServletRequest.hentAttributter(): Set<Attributt> {
        val ikkeStøttedeAttributter = mutableSetOf<Violation>()
        val støttedeAttributter = mutableSetOf<Attributt>()

        val etterspurteAttributter = getParameterValues(ATTRIBUTT_QUERY_NAVN)
            ?.filter { it.isNotBlank() }
            ?.map { it.lowercase() }
            ?.toSet() ?: emptySet()

        logger.info("Etterspurte Attributter = [${etterspurteAttributter.joinToString(", ")}]")

        etterspurteAttributter.forEach {
            try {
                støttedeAttributter.add(Attributt.fraApi(it))
            } catch (cause: Throwable) {
                ikkeStøttedeAttributter.add(
                    Violation(
                        parameterType = ParameterType.QUERY,
                        parameterName = ATTRIBUTT_QUERY_NAVN,
                        invalidValue = it,
                        reason = "Er ikke en støttet attributt."
                    )
                )
            }
        }

        return if (ikkeStøttedeAttributter.isEmpty()) støttedeAttributter
        else throw ValidationErrorResponseException(ikkeStøttedeAttributter)
    }

    private fun HttpServletRequest.hentFraOgMedTilOgMed(): Pair<LocalDate, LocalDate> {
        val fomQuery = getParameter(FRA_OG_MED_QUERY_NAVN)
        val tomQuery = getParameter(TIL_OG_MED_QUERY_NAVN)
        return Pair(
            first = if (fomQuery == null) iDag() else fomQuery.somLocalDate(FRA_OG_MED_QUERY_NAVN),
            second = if (tomQuery == null) iDag() else tomQuery.somLocalDate(TIL_OG_MED_QUERY_NAVN)
        )
    }

    private fun HttpServletRequest.hentOrganisasjoner(): Set<String> =
        getParameterValues(ORGANISASJONER)
            ?.filter { it.isNotBlank() }
            ?.map { it.lowercase() }
            ?.toSet() ?: emptySet()

    private fun String.somLocalDate(queryParameterName: String) = try {
        LocalDate.parse(this)
    } catch (cause: Throwable) {
        throw ValidationErrorResponseException(
            setOf(
                Violation(
                    parameterType = ParameterType.QUERY,
                    parameterName = queryParameterName,
                    invalidValue = this,
                    reason = "Må være på format yyyy-mm-dd."
                )
            )
        )
    }
}

internal fun <T : Any> json(body: T): ResponseEntity<T> =
    ResponseEntity.ok().contentType(jsonUtf8).body(body)

internal fun iDag(): LocalDate = LocalDate.now(ZoneId.of("Europe/Oslo"))
