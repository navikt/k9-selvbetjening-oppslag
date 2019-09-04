package no.nav.k9.inngaende.oppslag

import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import kotlinx.coroutines.withContext
import no.nav.k9.inngaende.RequestContextService
import no.nav.k9.inngaende.correlationId
import no.nav.k9.inngaende.idToken

internal fun Route.OppslagRoute(
    requestContextService: RequestContextService,
    oppslagService: OppslagService
) {

    get("/") {
        val attributter = call.request.queryParameters.getAll("attributter") ?: emptyList()
        if (attributter.isEmpty()) {
            // TODO: 400
        } else {
            val attr = attributter.somAttributter()

            val idToken = call.idToken()

            val oppslagResultat = withContext(coroutineContext + requestContextService.getCoroutineContext(
                correlationId = call.correlationId(),
                idToken = idToken
            )) {
                oppslagService.oppslag(
                    fødselsnummer = idToken.fødselsnummer,
                    attributter = attr
                )
            }
            call.respond(oppslagResultat.somJson(attr))
        }
    }
}
private fun List<String>.somAttributter() = map { Attributt.fraApi(it) }.toSet() // TODO: 400
