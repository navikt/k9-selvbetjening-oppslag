package no.nav.k9.integrasjon.pdl

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.net.ConnectException
import java.net.URI
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class PdlHttpTest {

    private val wireMock = WireMockServer(options().dynamicPort()).also { it.start() }
    private val connectionProvider = PdlHttp.connectionProvider()
    private val client = WebClient.builder()
        .clientConnector(ReactorClientHttpConnector(PdlHttp.httpClient(connectionProvider, responseTimeout = Duration.ofMillis(300))))
        .filter(PdlHttp.retryFilter())
        .build()

    @AfterEach
    fun rydd() {
        wireMock.stop()
        connectionProvider.dispose()
    }

    @Test
    fun `kall som timer ut prøves på nytt`() {
        sviktFørsteKall(aResponse().withFixedDelay(1000))

        assertEquals("svar", kallPdl())
        assertAntallKall(2)
    }

    @Test
    fun `kall som avbrytes uten svar prøves på nytt`() {
        sviktFørsteKall(aResponse().withFault(Fault.EMPTY_RESPONSE))

        assertEquals("svar", kallPdl())
        assertAntallKall(2)
    }

    @Test
    fun `kall som timer ut to ganger gir opprinnelig feil`() {
        wireMock.stubFor(post("/graphql").willReturn(aResponse().withFixedDelay(1000)))

        assertThrows<WebClientRequestException> { kallPdl() }
        assertAntallKall(2)
    }

    @Test
    fun `feilrespons fra PDL prøves ikke på nytt`() {
        wireMock.stubFor(post("/graphql").willReturn(serverError()))

        assertThrows<WebClientResponseException> { kallPdl() }
        assertAntallKall(1)
    }

    @Test
    fun `connect-feil prøves ikke på nytt`() {
        val kall = AtomicInteger()
        val exchange = ExchangeFunction {
            Mono.defer {
                kall.incrementAndGet()
                Mono.error(WebClientRequestException(ConnectException(), HttpMethod.POST, URI("http://pdl/graphql"), HttpHeaders()))
            }
        }

        assertThrows<WebClientRequestException> {
            PdlHttp.retryFilter().filter(ClientRequest.create(HttpMethod.POST, URI("http://pdl/graphql")).build(), exchange).block()
        }
        assertEquals(1, kall.get())
    }

    // Første kall får `feil`, neste kall får svaret «svar».
    private fun sviktFørsteKall(feil: ResponseDefinitionBuilder) {
        wireMock.stubFor(
            post("/graphql").inScenario("pdl").whenScenarioStateIs(Scenario.STARTED).willSetStateTo("ok").willReturn(feil)
        )
        wireMock.stubFor(post("/graphql").inScenario("pdl").whenScenarioStateIs("ok").willReturn(ok("svar")))
    }

    private fun kallPdl() = client.post().uri("${wireMock.baseUrl()}/graphql")
        .bodyValue("{}")
        .retrieve()
        .bodyToMono(String::class.java)
        .block()

    private fun assertAntallKall(antall: Int) = wireMock.verify(antall, postRequestedFor(urlEqualTo("/graphql")))
}
