package no.nav.k9

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.routing.Routing
import no.nav.helse.dusseldorf.ktor.core.DefaultProbeRoutes
import no.nav.k9.inngaende.JsonConverter
import no.nav.k9.inngaende.RequestContextService
import no.nav.k9.inngaende.oppslag.OppslagRoute
import no.nav.k9.inngaende.oppslag.OppslagService
import no.nav.k9.utgaende.cxf.WebServices
import no.nav.k9.utgaende.gateway.PersonV3Gateway
import java.net.URI

fun main(args: Array<String>): Unit  = io.ktor.server.netty.EngineMain.main(args)

fun Application.SelvbetjeningOppslag() {

    val requestContextService = RequestContextService()
    val webServices = WebServices(requestContextService)

    install(ContentNegotiation) {
        register(
            contentType = ContentType.Application.Json,
            converter = JsonConverter()
        )
    }
    install(Routing) {
        OppslagRoute(
            requestContextService = requestContextService,
            oppslagService = OppslagService(
                personV3Gateway = PersonV3Gateway(
                    personV3 = webServices.PersonV3(
                        serviceUrl = URI("https://wasapp-q1.adeo.no/tpsws/ws/Person/v3")
                    )
                )
            )
        )
        DefaultProbeRoutes()
    }
}