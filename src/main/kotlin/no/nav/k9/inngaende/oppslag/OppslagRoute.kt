package no.nav.k9.inngaende.oppslag

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.withContext
import no.nav.helse.dusseldorf.ktor.auth.idToken
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.k9.Ytelse
import no.nav.k9.inngaende.RequestContextService
import no.nav.k9.inngaende.correlationId
import no.nav.k9.utgaende.gateway.TilgangNektetException
import no.nav.k9.ytelseFraHeader
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate
import java.time.ZoneId

private const val ATTRIBUTT_QUERY_NAVN = "a"
private const val FRA_OG_MED_QUERY_NAVN = "fom"
private const val TIL_OG_MED_QUERY_NAVN = "tom"
private const val INKLUDER_ALLE_ANSETTELSESPERIODER = "inkluderAlleAnsettelsesperioder"
private const val ORGANISASJONER = "org"
private val tomJson = JSONObject()

private val logger: Logger = LoggerFactory.getLogger("OppslagRoute")

internal fun Route.OppslagRoute(
    requestContextService: RequestContextService,
    oppslagService: OppslagService,
) {

    get("/meg") {
        val attributter = call.hentAttributter()
        val ytelse = call.ytelseFraHeader()
        if (attributter.isEmpty()) {
            call.tomJsonResponse()
        } else {
            val idToken = call.idToken()
            val fraOgMedTilOgMed = call.hentFraOgMedTilOgMed()
            val inkluderAlleAnsettelsesperiode = call.inkluderAlleAnsettelsesperioder()

            try {
                val oppslagResultat: OppslagResultat = withContext(requestContextService.getCoroutineContext(
                    context = coroutineContext,
                    correlationId = call.correlationId(),
                    idToken = idToken
                )) {
                    oppslagService.oppslag(
                        ident = Ident(idToken.getNorskIdentifikasjonsnummer()),
                        attributter = attributter,
                        fraOgMed = fraOgMedTilOgMed.first,
                        tilOgMed = fraOgMedTilOgMed.second,
                        inkluderAlleAnsettelsesperiode,
                        ytelse = ytelse
                    )
                }
                call.respond(oppslagResultat.somJson(attributter))

            } catch (e: Exception) {
                when (e) {
                    is TilgangNektetException -> call.respondProblemDetails(
                        logger = logger,
                        problemDetails = DefaultProblemDetails(
                            title = "tilgangskontroll-feil",
                            status = 451,
                            instance = URI(call.request.path()),
                            detail = "Policy decision: ${e.policyException.decision} - Reason: ${e.policyException.reason}"
                        )
                    )
                    else -> throw e
                }
            }
        }
    }

    get("/arbeidsgivere") {
        val attributter = call.hentAttributter()
        if (attributter.isEmpty()) {
            call.tomJsonResponse()
        } else {
            val idToken = call.idToken()
            val etterspurteOrganisasjoner = call.hentOrganisasjoner().toSet()

            val oppslagResultat: OppslagResultat = withContext(requestContextService.getCoroutineContext(
                context = coroutineContext,
                correlationId = call.correlationId(),
                idToken = idToken
            )) {
                oppslagService.arbeidsgivere(
                    attributter = attributter,
                    organisasjoner = etterspurteOrganisasjoner
                )
            }
            call.respond(oppslagResultat.somJson(attributter))
        }
    }
}

private fun ApplicationCall.hentAttributter(): Set<Attributt> {
    val ikkeStøttedeAttributter = mutableSetOf<Violation>()
    val støttedeAttributter = mutableSetOf<Attributt>()

    val etterspurteAttributter = (request.queryParameters.getAll(ATTRIBUTT_QUERY_NAVN)
        ?.filter { it.isNotBlank() }
        ?.map { it.lowercase() }
        ?.toSet()) ?: emptySet()

    logger.info("Etterspurte Attributter = [${etterspurteAttributter.joinToString(", ")}]")

    etterspurteAttributter.forEach {
        try {
            støttedeAttributter.add(Attributt.fraApi(it))
        } catch (cause: Throwable) {
            ikkeStøttedeAttributter.add(Violation(
                parameterType = ParameterType.QUERY,
                parameterName = ATTRIBUTT_QUERY_NAVN,
                invalidValue = it,
                reason = "Er ikke en støttet attributt."
            ))
        }
    }

    return if (ikkeStøttedeAttributter.isEmpty()) {
        støttedeAttributter
    } else throw Throwblem(ValidationProblemDetails(ikkeStøttedeAttributter))
}

private fun ApplicationCall.hentFraOgMedTilOgMed(): Pair<LocalDate, LocalDate> {
    val fomQuery = request.queryParameters[FRA_OG_MED_QUERY_NAVN]
    val tomQuery = request.queryParameters[TIL_OG_MED_QUERY_NAVN]
    return Pair(
        first = if (fomQuery == null) iDag() else fomQuery.somLocalDate(FRA_OG_MED_QUERY_NAVN),
        second = if (tomQuery == null) iDag() else tomQuery.somLocalDate(TIL_OG_MED_QUERY_NAVN)
    )
}

private fun ApplicationCall.inkluderAlleAnsettelsesperioder(): Boolean {
    return request.queryParameters[INKLUDER_ALLE_ANSETTELSESPERIODER]?.toBoolean() == true
}

private fun ApplicationCall.ytelse(): Ytelse = ytelseFraHeader()


private fun ApplicationCall.hentOrganisasjoner(): Set<String> {
    return (request.queryParameters.getAll(ORGANISASJONER)
        ?.filter { it.isNotBlank() }
        ?.map { it.lowercase() }
        ?.toSet()) ?: emptySet()
}

private fun String.somLocalDate(queryParameterName: String) = try {
    LocalDate.parse(this)
} catch (cause: Throwable) {
    throw Throwblem(ValidationProblemDetails(setOf(Violation(
        parameterType = ParameterType.QUERY,
        parameterName = queryParameterName,
        invalidValue = this,
        reason = "Må være på format yyyy-mm-dd."
    ))))
}

private suspend fun ApplicationCall.tomJsonResponse() = respond(tomJson)
internal fun iDag() = LocalDate.now(ZoneId.of("Europe/Oslo"))
