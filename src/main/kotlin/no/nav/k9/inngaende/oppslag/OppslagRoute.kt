package no.nav.k9.inngaende.oppslag

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.withContext
import no.nav.helse.dusseldorf.ktor.core.ParameterType
import no.nav.helse.dusseldorf.ktor.core.Throwblem
import no.nav.helse.dusseldorf.ktor.core.ValidationProblemDetails
import no.nav.helse.dusseldorf.ktor.core.Violation
import no.nav.k9.inngaende.RequestContextService
import no.nav.k9.inngaende.correlationId
import no.nav.k9.inngaende.idToken
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZoneId

private const val ATTRIBUTT_QUERY_NAVN = "a"
private const val FRA_OG_MED_QUERY_NAVN = "fom"
private const val TIL_OG_MED_QUERY_NAVN = "tom"
private val tomJson = JSONObject()

private val logger: Logger = LoggerFactory.getLogger("OppslagRoute")

internal fun Route.OppslagRoute(
    requestContextService: RequestContextService,
    oppslagService: OppslagService
) {

    get("/meg") {
        val attributter = call.hentAttributter()
        if (attributter.isEmpty()) { call.tomJsonResponse() }
        else {
            val idToken = call.idToken()
            val fraOgMedTilOgMed = call.hentFraOgMedTilOgMed()

            val oppslagResultat: OppslagResultat = withContext(requestContextService.getCoroutineContext(
                context = coroutineContext,
                correlationId = call.correlationId(),
                idToken = idToken
            )) {
                oppslagService.oppslag(
                    ident = idToken.ident,
                    attributter = attributter,
                    fraOgMed = fraOgMedTilOgMed.first,
                    tilOgMed = fraOgMedTilOgMed.second
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
        try { støttedeAttributter.add(Attributt.fraApi(it)) }
        catch (cause: Throwable) {
            ikkeStøttedeAttributter.add(Violation(
                parameterType = ParameterType.QUERY,
                parameterName = ATTRIBUTT_QUERY_NAVN,
                invalidValue = it,
                reason = "Er ikke en støttet attributt."
            ))
        }
    }

    return if (ikkeStøttedeAttributter.isEmpty()) { støttedeAttributter }
    else throw Throwblem(ValidationProblemDetails(ikkeStøttedeAttributter))
}
private fun ApplicationCall.hentFraOgMedTilOgMed() : Pair<LocalDate, LocalDate> {
    val fomQuery = request.queryParameters[FRA_OG_MED_QUERY_NAVN]
    val tomQuery = request.queryParameters[TIL_OG_MED_QUERY_NAVN]
    return Pair(
        first = if (fomQuery == null) iDag() else fomQuery.somLocalDate(FRA_OG_MED_QUERY_NAVN),
        second = if (tomQuery == null) iDag() else tomQuery.somLocalDate(TIL_OG_MED_QUERY_NAVN)
    )
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
