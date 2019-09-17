package no.nav.k9

import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.features.CallId
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.metrics.micrometer.MicrometerMetrics
import io.ktor.routing.Routing
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.dusseldorf.ktor.auth.*
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.dusseldorf.ktor.metrics.MetricsRoute
import no.nav.helse.dusseldorf.ktor.metrics.init
import no.nav.k9.inngaende.JsonConverter
import no.nav.k9.inngaende.RequestContextService
import no.nav.k9.inngaende.oppslag.OppslagRoute
import no.nav.k9.inngaende.oppslag.OppslagService
import no.nav.k9.utgaende.gateway.AktoerRegisterV1Gateway
import no.nav.k9.utgaende.gateway.ArbeidsforholdV3Gateway
import no.nav.k9.utgaende.gateway.OrganisasjonV5Gateway
import no.nav.k9.utgaende.ws.WebServiceSTSClient
import no.nav.k9.utgaende.ws.WebServices
import no.nav.k9.utgaende.gateway.PersonV3Gateway
import no.nav.k9.utgaende.rest.AktørRegisterV1

fun main(args: Array<String>): Unit  = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
fun Application.SelvbetjeningOppslag() {
    val appId = environment.config.id()
    logProxyProperties()
    DefaultExports.initialize()

    val requestContextService = RequestContextService()
    val webServices = WebServices(
        requestContextService = requestContextService,
        stsClient = WebServiceSTSClient.instance(
            stsUrl = environment.config.wsStsUrl(),
            username = environment.config.wsUsername(),
            password = environment.config.wsPassword()
        )
    )
    val issuers = environment.config.issuers().withoutAdditionalClaimRules()

    install(Authentication) {
        multipleJwtIssuers(issuers)
    }

    install(ContentNegotiation) {
        register(
            contentType = ContentType.Application.Json,
            converter = JsonConverter()
        )
    }

    install(StatusPages) {
        DefaultStatusPages()
        AuthStatusPages()
    }

    install(CallIdRequired)

    install(Routing) {
        authenticate (*issuers.allIssuers()) {
            requiresCallId {
                OppslagRoute(
                    requestContextService = requestContextService,
                    oppslagService = OppslagService(
                        personV3Gateway = PersonV3Gateway(
                            personV3 = webServices.PersonV3(
                                serviceUrl = environment.config.personV3Url()
                            )
                        ),
                        aktoerRegisterV1Gateway = AktoerRegisterV1Gateway(
                            aktørRegisterV1 = AktørRegisterV1(environment.config.aktørV1Url())
                        ),
                        arbeidsforholdV3Gateway = ArbeidsforholdV3Gateway(
                            arbeidsforholdV3 = webServices.ArbeidsforholdV3(
                                serviceUrl = environment.config.arbeidsforholdV3Url()
                            )
                        ),
                        organisasjonV5Gateway = OrganisasjonV5Gateway(
                            organisasjonV5 = webServices.OrganisasjonV5(
                                serviceUrl = environment.config.organisasjonV5Url()
                            )
                        )
                    )
                )
            }
        }
        DefaultProbeRoutes()
        MetricsRoute()
    }

    install(MicrometerMetrics) {
        init(appId)
    }

    install(CallId) {
        fromXCorrelationIdHeader()
    }

    intercept(ApplicationCallPipeline.Monitoring) {
        call.request.log()
    }

    install(CallLogging) {
        correlationIdAndRequestIdInMdc()
        logRequests()
    }
}