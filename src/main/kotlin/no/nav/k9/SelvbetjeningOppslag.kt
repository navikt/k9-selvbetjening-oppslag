package no.nav.k9

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.metrics.micrometer.*
import io.ktor.routing.*
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.dusseldorf.ktor.auth.*
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.dusseldorf.ktor.metrics.MetricsRoute
import no.nav.helse.dusseldorf.ktor.metrics.init
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.inngaende.JsonConverter
import no.nav.k9.inngaende.RequestContextService
import no.nav.k9.inngaende.oppslag.OppslagRoute
import no.nav.k9.inngaende.oppslag.OppslagService
import no.nav.k9.utgaende.auth.AccessTokenClientResolver
import no.nav.k9.utgaende.gateway.*
import no.nav.k9.utgaende.rest.*
import no.nav.siftilgangskontroll.core.pdl.PdlService
import no.nav.siftilgangskontroll.core.tilgang.TilgangService
import java.util.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.SelvbetjeningOppslag() {
    val appId = environment.config.id()
    logProxyProperties()
    DefaultExports.initialize()

    val requestContextService = RequestContextService()
    val issuers = environment.config.issuers().withoutAdditionalClaimRules()
    val accessTokenClientResolver = AccessTokenClientResolver(environment.config.clients())

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

    val tokenxPdlApiExchangeTokenClient = CachedAccessTokenClient(accessTokenClientResolver.tokenxPdlApiExchangeTokenClient)
    val cachedAzureSystemTokenClient = CachedAccessTokenClient(accessTokenClientResolver.azurePdlApiSystemTokenClient)


    val pdlClient = GraphQLKtorClient(
        url = environment.config.pdlUrl().toURL(),
        httpClient = HttpClient(OkHttp) {
            defaultRequest {
                headers {
                    header(NavHeaders.Tema, "OMS")
                }
            }
        },
        serializer = GraphQLClientJacksonSerializer(objectMapper())
    )

    val tilgangService = TilgangService(
        pdlService = PdlService(graphQLClient = pdlClient)
    )

    install(Routing) {
        authenticate(*issuers.allIssuers()) {
            requiresCallId {
                OppslagRoute(
                    requestContextService = requestContextService,
                    oppslagService = OppslagService(
                        tpsProxyV1Gateway = TpsProxyV1Gateway(
                            tpsProxyV1 = TpsProxyV1(
                                baseUrl = environment.config.tpsProxyV1Url()
                            )
                        ),
                        pdlProxyGateway = PDLProxyGateway(
                            tilgangService = tilgangService,
                            cachedAccessTokenClient = tokenxPdlApiExchangeTokenClient,
                            pdlApiTokenxAudience = environment.config.pdlApiTokenxAudience(),
                            pdlApiAzureAudience = environment.config.pdlApiAzureAudience(),
                            cachedSystemTokenClient = cachedAzureSystemTokenClient
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

    environment.monitor.subscribe(ApplicationStopping) {
        CollectorRegistry.defaultRegistry.clear()
    }
}

fun objectMapper(): ObjectMapper {
    return jacksonObjectMapper()
        .dusseldorfConfigured()
        .enable(SerializationFeature.INDENT_OUTPUT)
}