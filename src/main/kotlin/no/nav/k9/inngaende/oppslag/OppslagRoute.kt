package no.nav.k9.inngaende.oppslag

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import kotlinx.coroutines.withContext
import no.nav.k9.inngaende.RequestContextService
import no.nav.k9.inngaende.correlationId
import no.nav.k9.inngaende.idToken
import java.time.LocalDate

internal fun Route.OppslagRoute(
    requestContextService: RequestContextService,
    oppslagService: OppslagService
) {

    get("/meg") {
        val attributter = call.request.queryParameters.getAll("a")?.filter { it.isNotBlank() } ?: emptyList()
        if (attributter.isEmpty()) {
            // TODO: 400
        } else {
            val attr = attributter.somAttributter()
            val fraOgMedTilOgMed = call.hentFraOgMedTilOgMed()

            val idToken = call.idToken()

            val oppslagResultat = withContext(requestContextService.getCoroutineContext(
                context = coroutineContext,
                correlationId = call.correlationId(),
                idToken = idToken
            )) {
                oppslagService.oppslag(
                    fødselsnummer = idToken.fødselsnummer,
                    attributter = attr,
                    fraOgMed = fraOgMedTilOgMed.first,
                    tilOgMed = fraOgMedTilOgMed.second
                )
            }
            call.respond(oppslagResultat.somJson(attr))
        }
    }
}
private fun List<String>.somAttributter() = map { Attributt.fraApi(it) }.toSet() // TODO: 400
private fun ApplicationCall.hentFraOgMedTilOgMed() : Pair<LocalDate, LocalDate> {
    val fomQuery = request.queryParameters["fom"]
    val tomQuery = request.queryParameters["tom"]
    return Pair( // TODO 400
        first = if (fomQuery == null) LocalDate.now() else LocalDate.parse(fomQuery),
        second = if (tomQuery == null) LocalDate.now() else LocalDate.parse(tomQuery)
    )
}