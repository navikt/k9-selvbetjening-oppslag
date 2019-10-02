package no.nav.k9

import com.typesafe.config.ConfigFactory
import io.ktor.config.ApplicationConfig
import io.ktor.config.HoconApplicationConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.contentType
import io.ktor.server.testing.createTestEnvironment
import io.ktor.server.testing.handleRequest
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.testsupport.jws.LoginService
import no.nav.helse.dusseldorf.ktor.testsupport.wiremock.WireMockBuilder
import no.nav.k9.wiremocks.k9SelvbetjeningOppslagConfig
import no.nav.k9.wiremocks.stubAktoerRegisterGetAktoerId
import no.nav.k9.wiremocks.stubTpsProxyGetPerson
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@KtorExperimentalAPI
class ApplicationTest {

    private companion object {

        private val logger: Logger = LoggerFactory.getLogger(ApplicationTest::class.java)

        val wireMockServer = WireMockBuilder()
            .withAzureSupport()
            .withNaisStsSupport()
            .withLoginServiceSupport()
            .k9SelvbetjeningOppslagConfig()
            .build()
            .stubAktoerRegisterGetAktoerId()
            .stubTpsProxyGetPerson()

        fun getConfig(): ApplicationConfig {

            val fileConfig = ConfigFactory.load()
            val testConfig = ConfigFactory.parseMap(TestConfiguration.asMap(wireMockServer = wireMockServer))
            val mergedConfig = testConfig.withFallback(fileConfig)

            return HoconApplicationConfig(mergedConfig)
        }


        val engine = TestApplicationEngine(createTestEnvironment {
            config = getConfig()
        })


        @BeforeAll
        @JvmStatic
        fun buildUp() {
            engine.start(wait = true)
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            logger.info("Tearing down")
            wireMockServer.stop()
            logger.info("Tear down complete")
        }
    }

    @Test
    fun `test isready, isalive og metrics`() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/isready") {}.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                handleRequest(HttpMethod.Get, "/isalive") {}.apply {
                    assertEquals(HttpStatusCode.OK, response.status())
                    handleRequest(HttpMethod.Get, "/metrics") {}.apply {
                        assertEquals(HttpStatusCode.OK, response.status())
                    }
                }
            }
        }
    }

    @Test
    fun `test megOppslag aktoerId`() {
        val idToken: String = LoginService.V1_0.generateJwt("01019012345")
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=aktør_id") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "meg-oppslag-aktoer-id")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.OK, response.status())
                kotlin.test.assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                { "aktør_id": "12345" }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }

        }
    }

    @Test
    fun `test megOppslag aktør_id og fornavn`() {
        val idToken: String = LoginService.V1_0.generateJwt("25037139184")
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=aktør_id&a=fornavn") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "meg-oppslag-aktoer-id-fornavn")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.OK, response.status())
                kotlin.test.assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                { "aktør_id": "12345",
                 "fornavn": "ARNE"}
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `test megOppslag aktør_id og navn og fødselsdato`() {
        val idToken: String = LoginService.V1_0.generateJwt("25037139184")
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=aktør_id&a=fornavn&a=mellomnavn&a=etternavn&a=fødselsdato") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "meg-oppslag-aktoer-id-navn-foedselsdato")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.OK, response.status())
                kotlin.test.assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                { 
                    "aktør_id": "12345",
                    "fornavn": "ARNE",
                    "mellomnavn": "BJARNE",
                    "etternavn": "CARLSEN",
                    "fødselsdato": "1990-01-02"
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

}
