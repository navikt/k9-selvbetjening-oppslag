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
import no.nav.k9.wiremocks.stubArbeidsgiverOgArbeidstakerRegister
import no.nav.k9.wiremocks.stubEnhetsRegister
import no.nav.k9.wiremocks.stubTpsProxyGetBarn
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
            .withNaisStsSupport()
            .withLoginServiceSupport()
            .k9SelvbetjeningOppslagConfig()
            .build()
            .stubAktoerRegisterGetAktoerId()
            .stubTpsProxyGetPerson()
            .stubTpsProxyGetBarn()
            .stubArbeidsgiverOgArbeidstakerRegister()
            .stubEnhetsRegister()

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
                { "aktør_id": "23456",
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
                    "aktør_id": "23456",
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

    @Test
    fun `test megOppslag navn har ikke mellomnavn`() {
        val idToken: String = LoginService.V1_0.generateJwt("01010067894")
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=fornavn&a=mellomnavn&a=etternavn") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "meg-oppslag-har-ikke-mellomnavn")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.OK, response.status())
                kotlin.test.assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                {
                    "fornavn": "CATO",
                    "etternavn": "NILSEN"
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `test barnOppslag aktoerId`() {
        val idToken: String = LoginService.V1_0.generateJwt("10047025546")
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=barn[].aktør_id") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "barn-oppslag-aktoer-id")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.OK, response.status())
                kotlin.test.assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                { 
                    "barn":[
                        {"aktør_id":"54321"}, 
                        {"aktør_id":"65432"}
                    ]
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true) //feiler. AktørId for barn blir satt til forelders aktørId
            }
        }
    }

    @Test
    fun `test barnOppslag navn og fødselsdato`() {
        val idToken: String = LoginService.V1_0.generateJwt("10047025546")
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn&a=barn[].fødselsdato") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "barn-oppslag-navn-foedselsdato")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.OK, response.status())
                kotlin.test.assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                { 
                    "barn":[
                        {
                            "fornavn": "KLØKTIG",
                            "mellomnavn": "BLUNKENDE",
                            "etternavn": "KONSOLL",
                            "fødselsdato": "2012-12-11"
                        },
                        {
                            "fornavn": "SLAPP",
                            "mellomnavn": "OVERSTRÅLENDE",
                            "etternavn": "HEST",
                            "fødselsdato": "2014-12-24"
                        }
                    ]
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `test barnOppslag navn har ikke mellomnavn`() {
        val idToken: String = LoginService.V1_0.generateJwt("01010067894")
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "barn-oppslag-har-ikke-mellomnavn")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.OK, response.status())
                kotlin.test.assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                { 
                    "barn":[
                        {
                            "fornavn": "MANGLER",
                            "etternavn": "MELLOMNAVN"
                        }
                    ]
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag orgnr`() {
        val idToken: String = LoginService.V1_0.generateJwt("01019012345")
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-orgnr")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.OK, response.status())
                kotlin.test.assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                {
                    "arbeidsgivere": {
                        "organisasjoner": [
                            {
                            "organisasjonsnummer": "123456789"
                            },
                            {
                            "organisasjonsnummer": "981585216"
                            }
                        ]
                    }
                 }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag orgnr og navn`() {
        val idToken: String = LoginService.V1_0.generateJwt("01019012345")
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-orgnr-navn")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.OK, response.status())
                kotlin.test.assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
            {
                "arbeidsgivere": {
                    "organisasjoner": [
                        {
                            "organisasjonsnummer": "123456789",
                            "navn": "DNB, FORSIKRING"
                        },
                        {
                            "organisasjonsnummer": "981585216",
                            "navn": "NAV FAMILIE- OG PENSJONSYTELSER"
                        }
                    ]
                }
             }
            """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }


    @Test
    fun `test oppslag alle attributter`() {
        val idToken: String = LoginService.V1_0.generateJwt("01019012345")
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=aktør_id&a=fornavn&a=mellomnavn&a=etternavn&a=fødselsdato" +
                    "&a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn&a=barn[].fødselsdato" +
                    "&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "oppslag-alle-attrib")
            }.apply {
                kotlin.test.assertEquals(HttpStatusCode.OK, response.status())
                kotlin.test.assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
            {
                "aktør_id": "12345",
                "fornavn": "STOR-KAR",
                "mellomnavn": "LANGEMANN",
                "etternavn": "TEST",
                "fødselsdato": "1985-07-27",
                "barn":[
                    {
                        "fornavn": "PRIPPEN",
                        "etternavn": "JUMBOJET",
                        "fødselsdato": "1999-12-11"
                    },
                    {
                        "fornavn": "MEGET",
                        "mellomnavn": "STILIG",
                        "etternavn": "PLANKE",
                        "fødselsdato": "2014-12-24"
                    }
                ],
                "arbeidsgivere": {
                    "organisasjoner": [
                        {
                            "organisasjonsnummer": "123456789",
                            "navn": "DNB, FORSIKRING"
                        },
                        {
                            "organisasjonsnummer": "981585216",
                            "navn": "NAV FAMILIE- OG PENSJONSYTELSER"
                        }
                    ]
                }
             }
            """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }
}
