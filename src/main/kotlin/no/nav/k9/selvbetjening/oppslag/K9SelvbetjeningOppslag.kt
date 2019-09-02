package no.nav.k9.selvbetjening.oppslag

import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.routing.Routing
import no.nav.helse.dusseldorf.ktor.core.DefaultProbeRoutes

fun main(args: Array<String>): Unit  = io.ktor.server.netty.EngineMain.main(args)

fun Application.K9SelvbetjeningOppslag() {
    install(Routing) {
        DefaultProbeRoutes()
    }
}