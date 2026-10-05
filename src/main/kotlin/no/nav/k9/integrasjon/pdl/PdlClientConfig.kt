package no.nav.k9.integrasjon.pdl

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.spring.GraphQLWebClient
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutException
import no.nav.k9.integrasjon.common.NavHeaders
import no.nav.siftilgangskontroll.core.pdl.PdlService
import no.nav.siftilgangskontroll.core.tilgang.TilgangService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import reactor.netty.http.client.HttpClient
import reactor.netty.http.client.PrematureCloseException
import reactor.netty.resources.ConnectionProvider
import reactor.util.retry.Retry
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

private val logger = LoggerFactory.getLogger(PdlClientConfig::class.java)

// Samme oppsett som sif-tilgangskontroll: GraphQLWebClient, PdlService og TilgangService som bønner.
@Configuration
internal class PdlClientConfig {

    @Bean(destroyMethod = "dispose")
    fun pdlConnectionProvider(): ConnectionProvider = PdlHttp.connectionProvider()

    // PDL_URL peker på selve GraphQL-endepunktet og brukes som den er.
    // Serializeren bygger sin egen mapper fra Boots JsonMapper og endrer ikke Boots mapper.
    @Bean
    fun pdlClient(
        webClientBuilder: WebClient.Builder,
        pdlConnectionProvider: ConnectionProvider,
        jsonMapper: JsonMapper,
        @Value("\${nav.register-urls.pdl-url}") pdlUrl: String,
    ) = GraphQLWebClient(
        url = pdlUrl,
        serializer = GraphQLClientJacksonSerializer(jsonMapper),
        builder = webClientBuilder
            .clientConnector(ReactorClientHttpConnector(PdlHttp.httpClient(pdlConnectionProvider)))
            .filter(PdlHttp.retryFilter())
            .defaultRequest { it.header(NavHeaders.Tema, "OMS") }
    )

    @Bean
    fun pdlService(pdlClient: GraphQLWebClient) = PdlService(graphQLClient = pdlClient)

    @Bean
    fun tilgangService(pdlService: PdlService) = TilgangService(pdlService = pdlService)
}

internal object PdlHttp {
    private val timeout = Duration.ofSeconds(10)

    // Brannmurer mellom GCP og FSS kan droppe idle TCP-connections uten å si fra, og da svarer PDL aldri.
    // Connections som har stått ubrukt i 30 s forkastes, slik at vi ikke gjenbruker en død connection.
    fun connectionProvider(): ConnectionProvider =
        ConnectionProvider.builder("pdl")
            .maxIdleTime(Duration.ofSeconds(30))
            .maxLifeTime(Duration.ofMinutes(5))
            .evictInBackground(Duration.ofSeconds(30))
            .metrics(true)
            .build()

    fun httpClient(
        connectionProvider: ConnectionProvider,
        responseTimeout: Duration = timeout,
    ): HttpClient = HttpClient.create(connectionProvider)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout.toMillis().toInt())
        .responseTimeout(responseTimeout)

    // Alle PDL-kall er GraphQL-spørringer (lesing), så de kan trygt sendes på nytt.
    fun retryFilter(): ExchangeFilterFunction =
        ExchangeFilterFunction { request, next ->
            next.exchange(request).retryWhen(
                Retry.fixedDelay(1, Duration.ofMillis(200))
                    .filter { it.erTimeoutEllerAvbruttConnection() }
                    .doBeforeRetry { signal ->
                        logger.warn("Kall mot PDL feilet ({}), prøver på nytt", signal.failure().cause?.javaClass?.simpleName)
                    }
                    .onRetryExhaustedThrow { _, signal -> signal.failure() }
            )
        }

    private fun Throwable.erTimeoutEllerAvbruttConnection() = this is WebClientRequestException &&
            generateSequence<Throwable>(this) { it.cause }.any { it is ReadTimeoutException || it is PrematureCloseException }
}
