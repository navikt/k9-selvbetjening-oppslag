package no.nav.k9

import com.typesafe.config.ConfigFactory
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.config.*
import io.ktor.server.testing.*
import io.prometheus.client.CollectorRegistry
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.k9.PersonFødselsnummer.DØD_PERSON
import no.nav.k9.PersonFødselsnummer.PERSON_1_MED_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_2_MED_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_3_MED_SKJERMET_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_4_MED_DØD_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_MED_FLERE_ARBEIDSFORHOLD_PER_ARBEIDSGIVER
import no.nav.k9.PersonFødselsnummer.PERSON_MED_FRILANS_OPPDRAG
import no.nav.k9.PersonFødselsnummer.PERSON_UNDER_MYNDIGHETS_ALDER
import no.nav.k9.PersonFødselsnummer.PERSON_UTEN_ARBEIDSGIVER
import no.nav.k9.PersonFødselsnummer.PERSON_UTEN_BARN
import no.nav.k9.TokenUtils.hentToken
import no.nav.k9.utgaende.rest.NavHeaders
import no.nav.k9.utgaende.rest.aaregv2.erAnsattIPerioden
import no.nav.k9.wiremocks.k9SelvbetjeningOppslagConfig
import no.nav.k9.wiremocks.stubArbeidsgiverOgArbeidstakerRegisterV2
import no.nav.k9.wiremocks.stubEnhetsRegister
import no.nav.k9.wiremocks.stubPDLRequest
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.siftilgangskontroll.core.pdl.utils.PdlOperasjon
import no.nav.siftilgangskontroll.pdl.generated.enums.IdentGruppe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate.parse
import java.util.*
import kotlin.test.assertFalse

class ApplicationTest {

    private companion object {

        private val logger: Logger = LoggerFactory.getLogger(ApplicationTest::class.java)

        val wireMockServer = WireMockBuilder()
            .withIDPortenSupport()
            .withTokendingsSupport()
            .withAzureSupport()
            .k9SelvbetjeningOppslagConfig()
            .build()
            .stubPDLRequest(PdlOperasjon.HENT_PERSON)
            .stubPDLRequest(PdlOperasjon.HENT_PERSON_BOLK)
            .stubPDLRequest(PdlOperasjon.HENT_IDENTER)
            .stubPDLRequest(PdlOperasjon.HENT_IDENTER_BOLK)
            .stubArbeidsgiverOgArbeidstakerRegisterV2()
            .stubEnhetsRegister()

        val mockOAuth2Server = MockOAuth2Server().apply { start() }

        fun getConfig(): ApplicationConfig {

            val fileConfig = ConfigFactory.load()
            val testConfig = ConfigFactory.parseMap(
                TestConfiguration.asMap(
                    wireMockServer = wireMockServer,
                    mockOAuth2Server = mockOAuth2Server
                )
            )
            val mergedConfig = testConfig.withFallback(fileConfig)

            return HoconApplicationConfig(mergedConfig)
        }


        @BeforeAll
        @JvmStatic
        fun buildUp() {
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            logger.info("Tearing down")
            wireMockServer.stop()
            mockOAuth2Server.shutdown()
            CollectorRegistry.defaultRegistry.clear()
            logger.info("Tear down complete")
        }
    }

    @Test
    fun `test isready, isalive og metrics`() {
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/isready").apply {
                assertEquals(HttpStatusCode.OK, status)
            }
            client.get("/isalive").apply {
                assertEquals(HttpStatusCode.OK, status)
            }
            client.get("/metrics").apply {
                assertEquals(HttpStatusCode.OK, status)
            }
        }
    }

    @Test
    fun `test oppslag uten idToken gir unauthorized`() {
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=aktør_id") {
                header(HttpHeaders.XCorrelationId, "meg-oppslag-uten-id-token")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, status)
            }
        }
    }

    @Test
    fun `test megOppslag aktoerId`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=aktør_id") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "meg-oppslag-aktoer-id")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
                { "aktør_id": "12345" }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `megOppslag med azure token skal gi 401 feil`() {

        val azureToken = mockOAuth2Server.issueToken(
            issuerId = "azure",
            subject = UUID.randomUUID().toString(),
            audience = "dev-fss:dusseldorf:k9-selvbetjening-oppslag",
            claims = mapOf("role" to "access_as_application")
        ).serialize()

        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=aktør_id") {
                header(HttpHeaders.Authorization, "Bearer $azureToken")
                header(HttpHeaders.XCorrelationId, "meg-oppslag-aktoer-id")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, status)
            }
        }
    }

    @Test
    fun `systemoppslag med azure token skal gi 200`() {
        val azureToken = mockOAuth2Server.issueToken(
            issuerId = "azure",
            subject = UUID.randomUUID().toString(),
            audience = "dev-fss:dusseldorf:k9-selvbetjening-oppslag",
            claims = mapOf("roles" to "access_as_application")
        ).serialize()

        testApplication {
            environment {
                config = getConfig()
            }
            client.post("/system/hent-identer") {
                header(HttpHeaders.Authorization, "Bearer $azureToken")
                header(HttpHeaders.XCorrelationId, "systemoppslag-hent-identer")
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.ContentType, "application/json")
                //language=json
                setBody(
                    """
                    {
                        "identer": ["$PERSON_1_MED_BARN"],
                        "identGrupper": ["${IdentGruppe.FOLKEREGISTERIDENT}"]
                    }
                """.trimIndent()
                )
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                //language=json
                val expectedResponse = """
                    [
                      {
                        "code": "ok",
                        "ident": "$PERSON_1_MED_BARN",
                        "identer": [
                          {
                            "ident": "$PERSON_1_MED_BARN",
                            "gruppe": "${IdentGruppe.FOLKEREGISTERIDENT}"
                          }
                        ]
                      }
                    ]
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `systemoppslag for å hente barn `() {
        val azureToken = mockOAuth2Server.issueToken(
            issuerId = "azure",
            subject = UUID.randomUUID().toString(),
            audience = "dev-fss:dusseldorf:k9-selvbetjening-oppslag",
            claims = mapOf("roles" to "access_as_application")
        ).serialize()

        testApplication {
            environment {
                config = getConfig()
            }
            client.post("/system/hent-barn") {
                header(HttpHeaders.Authorization, "Bearer $azureToken")
                header(HttpHeaders.XCorrelationId, "systemoppslag-hent-barn")
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.ContentType, "application/json")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
                //language=json
                setBody(
                    """
                    {
                        "identer": ["${BarnFødselsnummer.BARN_TIL_PERSON_1}"]
                    }
                """.trimIndent()
                )
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                //language=json
                val expectedResponse = """
                    [
                      {
                          "aktørId": {
                            "value": "54321"
                          },
                          "pdlBarn": {
                            "fornavn": "OLA",
                            "etternavn": "NORDMANN",
                            "ident": {
                              "value": "${BarnFødselsnummer.BARN_TIL_PERSON_1}"
                            },
                            "fødselsdato": "2012-02-24",
                            "adressebeskyttelse": []
                          }
                      }
                    ]
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `systemoppslag for å hente adressebeskyttet barn `() {
        val azureToken = mockOAuth2Server.issueToken(
            issuerId = "azure",
            subject = UUID.randomUUID().toString(),
            audience = "dev-fss:dusseldorf:k9-selvbetjening-oppslag",
            claims = mapOf("roles" to "access_as_application")
        ).serialize()

        testApplication {
            environment {
                config = getConfig()
            }
            client.post("/system/hent-barn") {
                header(HttpHeaders.Authorization, "Bearer $azureToken")
                header(HttpHeaders.XCorrelationId, "systemoppslag-hent-adresebeskyttet-barn")
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.ContentType, "application/json")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
                //language=json
                setBody(
                    """
                    {
                        "identer": ["${BarnFødselsnummer.SKJERMET_BARN_TIL_PERSON_3}"]
                    }
                """.trimIndent()
                )
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                //language=json
                val expectedResponse = """
                    [
                      {
                          "aktørId": {
                            "value": "666666"
                          },
                          "pdlBarn": {
                            "fornavn": "TVILSOM",
                            "mellomnavn": "GRADERT",
                            "etternavn": "VEPS",
                            "ident": {
                              "value": "${BarnFødselsnummer.SKJERMET_BARN_TIL_PERSON_3}"
                            },
                            "fødselsdato": "2012-10-27",
                            "adressebeskyttelse": [
                                {
                                    "gradering": "STRENGT_FORTROLIG"
                                }
                            ]
                          }
                      }
                    ]
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `test megOppslag aktør_id og fornavn`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_2_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=aktør_id&a=fornavn") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "meg-oppslag-aktoer-id-fornavn")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
                { "aktør_id": "23456",
                 "fornavn": "ARNE"}
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `test megOppslag aktør_id og navn og fødselsdato`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_2_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=aktør_id&a=fornavn&a=mellomnavn&a=etternavn&a=fødselsdato") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "meg-oppslag-aktoer-id-navn-foedselsdato")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
                { 
                    "aktør_id": "23456",
                    "fornavn": "ARNE",
                    "mellomnavn": "BJARNE",
                    "etternavn": "CARLSEN",
                    "fødselsdato": "1990-01-02"
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `test megOppslag navn har ikke mellomnavn`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = "01010067894")
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=fornavn&a=mellomnavn&a=etternavn") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "meg-oppslag-har-ikke-mellomnavn")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
                {
                    "fornavn": "CATO",
                    "mellomnavn": "",
                    "etternavn": "NILSEN"
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `gitt oppslag av død person, forvent feil`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = DØD_PERSON)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=fornavn&a=mellomnavn&a=etternavn") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "meg-oppslag-død-person")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError, status)
            }
        }
    }

    @Test
    fun `gitt oppslag av person under myndighetsalder (18), forvent feil`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_UNDER_MYNDIGHETS_ALDER)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=fornavn&a=mellomnavn&a=etternavn") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "meg-oppslag-død-person")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError, status)
            }
        }
    }

    @Test
    fun `test barnOppslag aktoerId`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_2_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=barn[].aktør_id") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "barn-oppslag-aktoer-id")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
                { 
                    "barn":[
                        {"aktør_id":"65432"}
                    ]
                }
                """.trimIndent()
                JSONAssert.assertEquals(
                    expectedResponse,
                    bodyAsText(),
                    true
                ) //feiler. AktørId for barn blir satt til forelders aktørId
            }
        }
    }

    @Test
    fun `test barnOppslag navn og fødselsdato`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_2_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get(
                "/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn&a=barn[].fødselsdato"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "barn-oppslag-navn-foedselsdato")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                // Første barn har totalt navn over > 24 tegn, så gjøres eget oppslag på navnet, den andre unngår oppslag da den er <= 24 tegn
                //language=json
                val expectedResponse = """
                { 
                    "barn":[
                        {
                            "fornavn": "TALENTFULL",
                            "mellomnavn": "MELLOMROM",
                            "etternavn": "STAUDE",
                            "fødselsdato": "2017-03-18"
                        }
                    ]
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `test barnOppslag navn har ikke mellomnavn`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "barn-oppslag-har-ikke-mellomnavn")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
                { 
                    "barn":[
                        {
                            "fornavn": "OLA",
                            "etternavn": "NORDMANN"
                        }
                    ]
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `gitt barn med strengt fortrolig adresse, forvent tom liste`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_3_MED_SKJERMET_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "barn-oppslag-har-ikke-mellomnavn")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
                { 
                    "barn": []
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `gitt død barn, forvent tom liste`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_4_MED_DØD_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "barn-oppslag-har-ikke-mellomnavn")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
                { 
                    "barn": []
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `test barnOppslag ingenBarn`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_UTEN_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "barn-oppslag-ingen-barn")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
                { 
                    "barn":[]
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag orgnr`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-orgnr")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
                {
                    "arbeidsgivere": {
                        "organisasjoner": [
                            {
                            "organisasjonsnummer": "123456789"
                            }
                        ]
                    }
                 }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag orgnr og navn`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get(
                "/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-orgnr-navn")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
            {
                "arbeidsgivere": {
                    "organisasjoner": [
                        {
                            "organisasjonsnummer": "123456789",
                            "navn": "DNB, FORSIKRING"
                        }
                    ]
                }
             }
            """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `Forvent organiasjon uten navn, gitt at navn ikke er funnet`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get(
                "/arbeidsgivere?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn&org=11111111"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-orgnr-navn")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                //language=json
                val expectedResponse = """
            {
                "arbeidsgivere": {
                    "organisasjoner": [
                        {
                            "organisasjonsnummer": "11111111"
                        }
                    ]
                }
             }
            """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `Forvent 1 organisasjon med navn, gitt organisasjonsnummer`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get(
                "/arbeidsgivere?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn&org=981585216"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-orgnr-navn")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                //language=json
                val expectedResponse = """
            {
                "arbeidsgivere": {
                    "organisasjoner": [
                        {
                            "organisasjonsnummer": "981585216",
                            "navn": "NAV FAMILIE- OG PENSJONSYTELSER"
                        }
                    ]
                }
             }
            """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `Forvent 2 organisasjoner med navn, gitt organisasjonsnummer`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get(
                "/arbeidsgivere?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn&org=981585216&org=67564534"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-orgnr-navn")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                //language=json
                val expectedResponse = """
            {
                "arbeidsgivere": {
                    "organisasjoner": [
                        {
                            "organisasjonsnummer": "981585216",
                            "navn": "NAV FAMILIE- OG PENSJONSYTELSER"
                        },
                        {
                            "organisasjonsnummer": "67564534",
                            "navn": "SELSKAP, MED, VELDIG, MANGE, NAVNELINJER"
                        }
                    ]
                }
             }
            """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag orgnr, navn, fom og tom`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get(
                "/meg?fom=2019-02-02&tom=2023-10-10&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn&a=arbeidsgivere[].organisasjoner[].ansettelsesperiode"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-orgnr-navn")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                //language=json
                val expectedResponse = """
                    {
                      "arbeidsgivere": {
                        "organisasjoner": [
                          {
                            "ansatt_fom": "2020-01-01",
                            "ansatt_tom": "2029-02-28",
                            "navn": "DNB, FORSIKRING",
                            "organisasjonsnummer": "123456789"
                          }
                        ]
                      }
                    }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag med ingen arbeidsgivere`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_UTEN_ARBEIDSGIVER)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get(
                "/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn" +
                        "&a=private_arbeidsgivere[].offentlig_ident&a=private_arbeidsgivere[].ansettelsesperiode"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-ingen-arbeidsgiver")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
            {
                "arbeidsgivere":{
                    "organisasjoner":[],
                    "private_arbeidsgivere":[]
                }
            }
            """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `tester oppslag av private arbeidsgivere`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get(
                "/meg?a=private_arbeidsgivere[].offentlig_ident&a=private_arbeidsgivere[].ansettelsesperiode"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-private-arbeidsgivere")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
                    {
                      "arbeidsgivere": {
                        "private_arbeidsgivere": [
                          {
                            "offentlig_ident": "28837996386",
                            "ansatt_fom": "2020-01-01",
                            "ansatt_tom": "2029-02-28"
                          }
                        ]
                      }
                    }
                    """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `Forventer å kun få unike arbeidsgivere selvom man har flere arbeidsforhold hos en arbeidsgiver`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_MED_FLERE_ARBEIDSFORHOLD_PER_ARBEIDSGIVER)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get(
                "/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn" +
                        "&a=private_arbeidsgivere[].offentlig_ident&a=private_arbeidsgivere[].ansettelsesperiode"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-arbeidsgivere")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
                    {
                      "arbeidsgivere": {
                        "private_arbeidsgivere": [
                          {
                            "ansatt_fom": "2020-01-01",
                            "offentlig_ident": "28837996386",
                            "ansatt_tom": "2029-02-28"
                          }
                        ],
                        "organisasjoner": [
                          {
                            "navn": "DNB, FORSIKRING",
                            "organisasjonsnummer": "123456789"
                          }
                        ]
                      }
                    }
                    """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `Forventer flere ansettelsesperioder hos arbeidsgiver`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_MED_FLERE_ARBEIDSFORHOLD_PER_ARBEIDSGIVER)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get(
                "/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn&a=arbeidsgivere[].organisasjoner[].ansettelsesperiode&inkluderAlleAnsettelsesperioder=true&fom=2014-01-01"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-arbeidsgivere")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
                    {
                      "arbeidsgivere": {
                        "organisasjoner": [
                          {
                            "navn": "DNB, FORSIKRING",
                            "organisasjonsnummer": "123456789",
                            "ansatt_fom": "2015-01-01",
                            "ansatt_tom": "2019-12-31"
                          },
                          {
                            "navn": "DNB, FORSIKRING",
                            "organisasjonsnummer": "123456789",
                            "ansatt_fom": "2020-01-01",
                            "ansatt_tom": "2029-02-28"
                          }
                        ]
                      }
                    }
                    """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }


    @Test
    fun `Teste oppslag av frilans oppdrag`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_MED_FRILANS_OPPDRAG)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get(
                "/meg?a=frilansoppdrag[]"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-frilans-oppdrag")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
                    {
                      "arbeidsgivere": {
                        "frilansoppdrag": [
                          {
                            "type": "Person",
                            "ansatt_fom": "2020-01-01",
                            "ansatt_tom": "2029-02-28",
                            "offentlig_ident": "28837996386"
                          },
                          {
                            "type": "Organisasjon",
                            "ansatt_fom": "2020-01-01",
                            "ansatt_tom": "2029-02-28",
                            "organisasjonsnummer": "123456789",
                            "navn": "DNB, FORSIKRING"
                          }
                        ]
                      }
                    }
                    """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `test oppslag alle attributter`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get(
                "/meg?fom=2019-09-09&tom=2022-10-10" +
                        "&a=aktør_id&a=fornavn&a=mellomnavn&a=etternavn&a=fødselsdato" +
                        "&a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn&a=barn[].fødselsdato&a=barn[].har_samme_adresse&a=barn[].identitetsnummer" +
                        "&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn" +
                        "&a=private_arbeidsgivere[].offentlig_ident&a=private_arbeidsgivere[].ansettelsesperiode&a=frilansoppdrag[]"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "oppslag-alle-attrib")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
                    {
                      "mellomnavn": "LANGEMANN",
                      "etternavn": "TEST",
                      "arbeidsgivere": {
                        "private_arbeidsgivere": [
                          {
                            "ansatt_fom": "2020-01-01",
                            "offentlig_ident": "28837996386",
                            "ansatt_tom": "2029-02-28"
                          }
                        ],
                        "frilansoppdrag": [
                          {
                            "ansatt_fom": "2020-01-01",
                            "offentlig_ident": "28837996386",
                            "type": "Person",
                            "ansatt_tom": "2029-02-28"
                          },
                          {
                            "ansatt_fom": "2020-01-01",
                            "navn": "DNB, FORSIKRING",
                            "type": "Organisasjon",
                            "organisasjonsnummer": "123456789",
                            "ansatt_tom": "2029-02-28"
                          }
                        ],
                        "organisasjoner": [
                          {
                            "navn": "DNB, FORSIKRING",
                            "organisasjonsnummer": "123456789"
                          }
                        ]
                      },
                      "barn": [
                        {
                          "etternavn": "NORDMANN",
                          "identitetsnummer": "11129998665",
                          "fødselsdato": "2012-02-24",
                          "fornavn": "OLA"
                        }
                      ],
                      "fødselsdato": "1985-07-27",
                      "fornavn": "STOR-KAR",
                      "aktør_id": "12345"
                    }
            """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `test oppslag ingen attributter skal returnere tom JSON`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "oppslag-ingen-attrib")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("application/json; charset=UTF-8", contentType().toString())
                val expectedResponse = """
                {}
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `test oppslag bare ugyldig attributt - bad request`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=ugyldigAttrib") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "oppslag-ugyldig-attrib")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, status)
                assertEquals("application/problem+json", contentType().toString())
                val expectedResponse = """
                {
                    "detail":"Requesten inneholder ugyldige paramtere.",
                    "instance":"about:blank",
                    "type":"/problem-details/invalid-request-parameters",
                    "title":"invalid-request-parameters",
                    "invalid_parameters":[
                        {"name":"a","reason":"Er ikke en støttet attributt.","invalid_value":"ugyldigattrib","type":"query"}
                    ],
                    "status":400
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `test oppslag ugyldige attributt - bad request`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=aktør_id&a=ugyldigattrib&a=fornavn&a=annetugyldigattrib") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "oppslag-ugyldige-attrib")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, status)
                assertEquals("application/problem+json", contentType().toString())
                val expectedResponse = """
                {
                    "detail":"Requesten inneholder ugyldige paramtere.",
                    "instance":"about:blank",
                    "type":"/problem-details/invalid-request-parameters",
                    "title":"invalid-request-parameters",
                    "invalid_parameters":[
                        {"name":"a","reason":"Er ikke en støttet attributt.","invalid_value":"ugyldigattrib","type":"query"},
                        {"name":"a","reason":"Er ikke en støttet attributt.","invalid_value":"annetugyldigattrib","type":"query"}
                    ],
                    "status":400
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `gitt oppslag av søker under myndighetsalder, forvent 451 Unavailable For Legal Reasons`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_UNDER_MYNDIGHETS_ALDER)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get("/meg?a=aktør_id") {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "oppslag-ugyldige-attrib")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(451, status.value)
                assertEquals("application/problem+json", contentType().toString())
                //language=json
                val expectedResponse = """
                {
                    "detail": "Policy decision: DENY - Reason: (NAV-bruker er i live AND NAV-bruker er ikke myndig)",
                    "instance": "/meg",
                    "type": "/problem-details/tilgangskontroll-feil",
                    "title": "tilgangskontroll-feil",
                    "status": 451
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag feil format fom`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get(
                "/meg?fom=2019/02/02&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "oppslag-feil-format-fom")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, status)
                assertEquals("application/problem+json", contentType().toString())
                val expectedResponse = """
                {
                    "detail":"Requesten inneholder ugyldige paramtere.",
                    "instance":"about:blank",
                    "type":"/problem-details/invalid-request-parameters",
                    "title":"invalid-request-parameters",
                    "invalid_parameters":[
                        {"name":"fom","reason":"Må være på format yyyy-mm-dd.","invalid_value":"2019/02/02","type":"query"}
                    ],
                    "status":400
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag feil format tom`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        testApplication {
            environment {
                config = getConfig()
            }
            client.get(
                "/meg?fom=2019-02-02&tom=2019.10.10&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer"
            ) {
                header(HttpHeaders.Authorization, "Bearer $idToken")
                header(HttpHeaders.XCorrelationId, "oppslag-feil-format-tom")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, status)
                assertEquals("application/problem+json", contentType().toString())
                val expectedResponse = """
                {
                    "detail":"Requesten inneholder ugyldige paramtere.",
                    "instance":"about:blank",
                    "type":"/problem-details/invalid-request-parameters",
                    "title":"invalid-request-parameters",
                    "invalid_parameters":[
                        {"name":"tom","reason":"Må være på format yyyy-mm-dd.","invalid_value":"2019.10.10","type":"query"}
                    ],
                    "status":400
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, bodyAsText(), true)
            }
        }
    }

    @Test
    fun `Test av erAnsattIPerioden`() {
        val mandag = parse("2022-01-07")
        val tirsdag = mandag.plusDays(1)
        val onsdag = tirsdag.plusDays(1)
        val torsdag = onsdag.plusDays(1)
        val fredag = torsdag.plusDays(1)

        val ansattFOM = tirsdag
        val ansattTOM = torsdag

        assertTrue(erAnsattIPerioden(ansattFOM, ansattTOM, tirsdag, torsdag)) //Samme dato
        assertTrue(erAnsattIPerioden(ansattFOM, ansattTOM, tirsdag, fredag)) //En dag ekstra TOM
        assertTrue(erAnsattIPerioden(ansattFOM, ansattTOM, mandag, torsdag)) //En dag ekstra FOM
        assertTrue(erAnsattIPerioden(ansattFOM, ansattTOM, onsdag, fredag)) //Overlapp midt i
        assertTrue(erAnsattIPerioden(ansattFOM, ansattTOM, mandag, fredag)) //Full overlapp

        assertFalse(erAnsattIPerioden(ansattFOM, ansattTOM, mandag, mandag)) //En dag før
        assertFalse(erAnsattIPerioden(ansattFOM, ansattTOM, fredag, fredag)) //En dag etter

        assertTrue(erAnsattIPerioden(ansattFOM, null, fredag, fredag)) //Har ingen TOM
    }
}
