package no.nav.k9.inngaende.oppslag

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.withContext
import no.nav.helse.dusseldorf.ktor.auth.idToken
import no.nav.k9.inngaende.RequestContextService
import no.nav.k9.inngaende.correlationId
import no.nav.k9.objectMapper
import no.nav.siftilgangskontroll.pdl.generated.enums.IdentGruppe

internal fun Route.SystemOppslagRoute(
    requestContextService: RequestContextService,
    systemOppslagService: SystemOppslagService,
) {

    post("/system/hent-identer") {
        val idToken = call.idToken()
        val hentIdenterForespørsel = objectMapper().readValue(call.receive<String>(), HentIdenterForespørsel::class.java)

        withContext(requestContextService.getCoroutineContext(
            context = coroutineContext,
            correlationId = call.correlationId(),
            idToken = idToken
        )) {
            val hentIdenterBolkResults = systemOppslagService.hentIdenter(
                identer = hentIdenterForespørsel.identer,
                identGrupper = hentIdenterForespørsel.identGrupper
            )

            call.respond(hentIdenterBolkResults)
        }
    }
}

internal data class HentIdenterForespørsel(
    val identer: List<String>,
    val identGrupper: List<IdentGruppe>
)
