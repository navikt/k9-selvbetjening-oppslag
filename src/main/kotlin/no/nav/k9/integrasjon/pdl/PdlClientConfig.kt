package no.nav.k9.integrasjon.pdl

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.channel.ChannelOption
import no.nav.k9.integrasjon.common.NavHeaders
import no.nav.siftilgangskontroll.core.pdl.PdlService
import no.nav.siftilgangskontroll.core.tilgang.TilgangService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration

private val pdlTimeout = Duration.ofSeconds(10)

// Samme oppsett som sif-tilgangskontroll: GraphQLWebClient, PdlService og TilgangService som bønner.
@Configuration
internal class PdlClientConfig {

    // PDL_URL peker på selve GraphQL-endepunktet og brukes som den er.
    // Serializeren endrer mapperen den får, så vi gir den en kopi av Boots mapper.
    @Bean
    fun pdlClient(
        webClientBuilder: WebClient.Builder,
        objectMapper: ObjectMapper,
        @Value("\${nav.register-urls.pdl-url}") pdlUrl: String,
    ) = GraphQLWebClient(
        url = pdlUrl,
        serializer = GraphQLClientJacksonSerializer(objectMapper.copy()),
        builder = webClientBuilder
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, pdlTimeout.toMillis().toInt())
                        .responseTimeout(pdlTimeout)
                )
            )
            .defaultRequest { it.header(NavHeaders.Tema, "OMS") }
    )

    @Bean
    fun pdlService(pdlClient: GraphQLWebClient) = PdlService(graphQLClient = pdlClient)

    @Bean
    fun tilgangService(pdlService: PdlService) = TilgangService(pdlService = pdlService)
}
