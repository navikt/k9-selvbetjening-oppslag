package no.nav.k9

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.hotspot.DefaultExports
import no.nav.helse.dusseldorf.ktor.auth.AuthStatusPages
import no.nav.helse.dusseldorf.ktor.auth.clients
import no.nav.helse.dusseldorf.ktor.auth.idToken
import no.nav.helse.dusseldorf.ktor.core.*
import no.nav.helse.dusseldorf.ktor.jackson.dusseldorfConfigured
import no.nav.helse.dusseldorf.ktor.metrics.MetricsRoute
import no.nav.helse.dusseldorf.ktor.metrics.init
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.inngaende.JsonConverter
import no.nav.k9.inngaende.RequestContextService
import no.nav.k9.inngaende.oppslag.*
import no.nav.k9.inngaende.oppslag.ArbeidsgivereOppslag
import no.nav.k9.inngaende.oppslag.OppslagRoute
import no.nav.k9.inngaende.oppslag.OppslagService
import no.nav.k9.inngaende.oppslag.SystemOppslagRoute
import no.nav.k9.utgaende.auth.AccessTokenClientResolver
import no.nav.k9.utgaende.gateway.ArbeidsgiverOgArbeidstakerRegisterGateway
import no.nav.k9.utgaende.gateway.EnhetsregisterV1Gateway
import no.nav.k9.utgaende.gateway.PDLProxyGateway
import no.nav.k9.utgaende.rest.aaregv2.ArbeidsgiverOgArbeidstakerRegisterV2
import no.nav.k9.utgaende.rest.EnhetsregisterV1
import no.nav.k9.utgaende.rest.NavHeaders
import no.nav.security.token.support.v3.RequiredClaims
import no.nav.security.token.support.v3.asIssuerProps
import no.nav.security.token.support.v3.tokenValidationSupport
import no.nav.siftilgangskontroll.core.pdl.PdlService
import no.nav.siftilgangskontroll.core.tilgang.TilgangService
import org.slf4j.LoggerFactory

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.SelvbetjeningOppslag() {
    val logger = LoggerFactory.getLogger("no.nav.k9.SelvbetjeningOppslagKt.SelvbetjeningOppslag")
    val appId = environment.config.id()
    logProxyProperties()
    DefaultExports.initialize()

    val applicationConfig = this.environment.config
    val allIssuers = applicationConfig.asIssuerProps().keys

    val requestContextService = RequestContextService()
    val accessTokenClientResolver = AccessTokenClientResolver(environment.config.clients())

    install(Authentication) {
        allIssuers
            .filterNot { it == "azure" }
            .forEach { issuer: String ->
                tokenValidationSupport(
                    name = issuer,
                    config = applicationConfig,
                    requiredClaims = RequiredClaims(
                        issuer = issuer,
                        claimMap = arrayOf("acr=Level4")
                    )
                )
            }

        allIssuers
            .filter { it == "azure" }
            .forEach { issuer: String ->
                tokenValidationSupport(
                    name = issuer,
                    config = applicationConfig,
                    requiredClaims = RequiredClaims(
                        issuer = issuer,
                        claimMap = arrayOf("roles=access_as_application")
                    )
                )
            }
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

    val tokenxExchangeTokenClient = CachedAccessTokenClient(accessTokenClientResolver.tokenxExchangeTokenClient)
    val cachedAzureSystemTokenClient = CachedAccessTokenClient(accessTokenClientResolver.azureSystemTokenClient)

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

    val pdlProxyGateway = PDLProxyGateway(
        tilgangService = tilgangService,
        cachedAccessTokenClient = tokenxExchangeTokenClient,
        pdlApiTokenxAudience = environment.config.pdlApiTokenxAudience(),
        pdlApiAzureAudience = environment.config.pdlApiAzureAudience(),
        cachedSystemTokenClient = cachedAzureSystemTokenClient
    )
    routing {
        authenticate(
            configurations = allIssuers.filter { issuer -> issuer != "azure" }.toTypedArray()
        ) {
            requiresCallId {
                OppslagRoute(
                    requestContextService = requestContextService,
                    oppslagService = OppslagService(
                        megOppslag = MegOppslag(pdlProxyGateway),
                        barnOppslag = BarnOppslag(pdlProxyGateway),
                        arbeidsgiverOppslag = ArbeidsgivereOppslag(
                            enhetsregisterV1Gateway = EnhetsregisterV1Gateway(
                                enhetsregisterV1 = EnhetsregisterV1(
                                    baseUrl = applicationConfig.enhetsregisterV1Url()
                                )
                            )
                        ),
                        arbeidsgiverOgArbeidstakerRegisterGateway = ArbeidsgiverOgArbeidstakerRegisterGateway(
                            arbeidstakerOgArbeidstakerRegisterV2 = ArbeidsgiverOgArbeidstakerRegisterV2(
                                baseUrl = applicationConfig.arbeidsgiverOgArbeidstakerV2Url(),
                                cachedAccessTokenClient = tokenxExchangeTokenClient,
                                aaregTokenxAudience = applicationConfig.aaregTokenxAudience()
                            )
                        )
                    )
                )
            }
        }

        // Tillater kun azure issuer. Ment for systemkall.
        authenticate(
            configurations = allIssuers.filter { issuer -> issuer == "azure" }.toTypedArray()
        ) {
            requiresCallId {
                SystemOppslagRoute(
                    requestContextService = requestContextService,
                    systemOppslagService = SystemOppslagService(pdlProxyGateway = pdlProxyGateway)
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
        mdc("id_token_jti") { call ->
            try {
                val idToken = call.idToken()
                logger.info("Issuer [{}]", idToken.issuer())
                idToken.getId()
            } catch (cause: Throwable) {
                null
            }
        }
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
