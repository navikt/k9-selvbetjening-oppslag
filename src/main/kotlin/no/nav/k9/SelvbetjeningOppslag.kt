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
import no.nav.k9.utgaende.gateway.*
import no.nav.k9.utgaende.gateway.AktoerRegisterV1Gateway
import no.nav.k9.utgaende.gateway.EnhetsregisterV1Gateway
import no.nav.k9.utgaende.rest.AktoerregisterV1
import no.nav.k9.utgaende.rest.ArbeidsgiverOgArbeidstakerRegisterV1
import no.nav.k9.utgaende.rest.BrregProxyV1
import no.nav.k9.utgaende.rest.EnhetsregisterV1
import no.nav.k9.utgaende.rest.NaisStsAccessTokenClient
import no.nav.k9.utgaende.rest.TpsProxyV1

fun main(args: Array<String>): Unit  = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
fun Application.SelvbetjeningOppslag() {
    val appId = environment.config.id()
    logProxyProperties()
    DefaultExports.initialize()

    val requestContextService = RequestContextService()
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


    val naisStsAccessTokenClient = NaisStsAccessTokenClient(
        tokenEndpoint = environment.config.restTokenUrl(),
        clientId = environment.config.clientId(),
        clientSecret = environment.config.clientSecret()
    )

    install(Routing) {
        authenticate (*issuers.allIssuers()) {
            requiresCallId {
                OppslagRoute(
                    requestContextService = requestContextService,
                    oppslagService = OppslagService(
                        tpsProxyV1Gateway = TpsProxyV1Gateway(
                            tpsProxyV1 = TpsProxyV1(
                                baseUrl = environment.config.tpsProxyV1Url(),
                                accessTokenClient = naisStsAccessTokenClient
                            )
                        ),
                        aktoerRegisterV1Gateway = AktoerRegisterV1Gateway(
                            aktørRegisterV1 = AktoerregisterV1(
                                baseUrl = environment.config.aktørV1Url(),
                                accessTokenClient = naisStsAccessTokenClient
                            )
                        ),
                        enhetsregisterV1Gateway = EnhetsregisterV1Gateway(
                            enhetsregisterV1 = EnhetsregisterV1(
                                baseUrl = environment.config.enhetsregisterV1Url()
                            )
                        ),
                        arbeidsgiverOgArbeidstakerRegisterV1Gateway = ArbeidsgiverOgArbeidstakerRegisterV1Gateway(
                            arbeidstakerOgArbeidstakerRegisterV1 = ArbeidsgiverOgArbeidstakerRegisterV1(
                                baseUrl = environment.config.arbeidsgiverOgArbeidstakerV1Url(),
                                accessTokenClient = naisStsAccessTokenClient
                            )
                        ),
                        brregProxyV1Gateway = BrregProxyV1Gateway(
                            brregProxyV1 = BrregProxyV1(
                                baseUrl = environment.config.brregProxyV1Url(),
                                accessTokenClient = naisStsAccessTokenClient
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