package no.nav.k9

import com.typesafe.config.ConfigFactory
import io.ktor.config.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.prometheus.client.CollectorRegistry
import no.nav.helse.dusseldorf.testsupport.jws.LoginService
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.k9.BarnFødselsnummer.BARN_TIL_PERSON_1
import no.nav.k9.PersonFødselsnummer.DØD_PERSON
import no.nav.k9.PersonFødselsnummer.PERSON_1_MED_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_2_MED_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_3_MED_SKJERMET_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_4_MED_DØD_BARN
import no.nav.k9.PersonFødselsnummer.PERSON_UNDER_MYNDIGHETS_ALDER
import no.nav.k9.inngaende.oppslag.MegUrlGenerator
import no.nav.k9.wiremocks.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class ApplicationTest {

    private companion object {

        private val logger: Logger = LoggerFactory.getLogger(ApplicationTest::class.java)

        val wireMockServer = WireMockBuilder()
            .withNaisStsSupport()
            .withLoginServiceSupport()
            .k9SelvbetjeningOppslagConfig()
            .build()
            .stubPDLHentIdent()
            .stubPDLHentPerson()
            .stubPDLHentBarn()
            .stubTpsProxyGetPerson()
            .stubTpsProxyGetBarn()
            .stubArbeidsgiverOgArbeidstakerRegister()
            .stubEnhetsRegister()
            .stubTpsProxyGetNavn()
            .stubBrregProxyV1()

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
            CollectorRegistry.defaultRegistry.clear()
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
    fun `test oppslag uten idToken gir unauthorized`() {
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=aktør_id") {
                addHeader(HttpHeaders.XCorrelationId, "meg-oppslag-uten-id-token")
            }.apply {
                assertEquals(HttpStatusCode.Unauthorized, response.status())
            }
        }
    }

    @Test
    fun `test megOppslag aktoerId`() {
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_1_MED_BARN)
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=aktør_id") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "meg-oppslag-aktoer-id")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                { "aktør_id": "12345" }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `test megOppslag aktør_id og fornavn`() {
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_2_MED_BARN)
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=aktør_id&a=fornavn") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "meg-oppslag-aktoer-id-fornavn")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
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
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_2_MED_BARN)
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=aktør_id&a=fornavn&a=mellomnavn&a=etternavn&a=fødselsdato") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "meg-oppslag-aktoer-id-navn-foedselsdato")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
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
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                {
                    "fornavn": "CATO",
                    "mellomnavn": "",
                    "etternavn": "NILSEN"
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `gitt oppslag av død person, forvent feil`() {
        val idToken: String = LoginService.V1_0.generateJwt(DØD_PERSON)
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=fornavn&a=mellomnavn&a=etternavn") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "meg-oppslag-død-person")
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
            }
        }
    }

    @Test
    fun `gitt oppslag av person under myndighetsalder (18), forvent feil`() {
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_UNDER_MYNDIGHETS_ALDER)
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=fornavn&a=mellomnavn&a=etternavn") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "meg-oppslag-død-person")
            }.apply {
                assertEquals(HttpStatusCode.InternalServerError, response.status())
            }
        }
    }

    @Test
    fun `test barnOppslag aktoerId`() {
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_2_MED_BARN)
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=barn[].aktør_id") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "barn-oppslag-aktoer-id")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                { 
                    "barn":[
                        {"aktør_id":"65432"}
                    ]
                }
                """.trimIndent()
                JSONAssert.assertEquals(
                    expectedResponse,
                    response.content!!,
                    true
                ) //feiler. AktørId for barn blir satt til forelders aktørId
            }
        }
    }

    @Test
    fun `test barnOppslag navn og fødselsdato`() {
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_2_MED_BARN)
        with(engine) {
            handleRequest(
                HttpMethod.Get,
                "/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn&a=barn[].fødselsdato"
            ) {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "barn-oppslag-navn-foedselsdato")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
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
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `test barnOppslag navn har ikke mellomnavn`() {
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_1_MED_BARN)
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "barn-oppslag-har-ikke-mellomnavn")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
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
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `gitt barn med strengt fortrolig adresse, forvent tom liste`() {
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_3_MED_SKJERMET_BARN)
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "barn-oppslag-har-ikke-mellomnavn")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                { 
                    "barn": []
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `gitt død barn, forvent tom liste`() {
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_4_MED_DØD_BARN)
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "barn-oppslag-har-ikke-mellomnavn")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                { 
                    "barn": []
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `test barnOppslag ingenBarn`() {
        val idToken: String = LoginService.V1_0.generateJwt("10047025546")
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "barn-oppslag-ingen-barn")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                { 
                    "barn":[]
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag orgnr`() {
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_1_MED_BARN)
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-orgnr")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
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
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_1_MED_BARN)
        with(engine) {
            handleRequest(
                HttpMethod.Get,
                "/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn"
            ) {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-orgnr-navn")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
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
    fun `test arbeidsgiverOppslag orgnr, navn, fom og tom`() {
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_1_MED_BARN)
        with(engine) {
            handleRequest(
                HttpMethod.Get,
                "/meg?fom=2019-02-02&tom=2019-10-10&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn"
            ) {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-orgnr-navn")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
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
    fun `test arbeidsgiverOppslag ingenArbeidsgiver`() {
        val idToken: String = LoginService.V1_0.generateJwt("02029212345")
        with(engine) {
            handleRequest(
                HttpMethod.Get,
                "/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn"
            ) {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "arbeidsgiver-oppslag-ingen-arbeidsgiver")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
            {
                "arbeidsgivere":{
                    "organisasjoner":[]
                }
            }
            """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `test oppslag alle attributter`() {
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_1_MED_BARN)
        with(engine) {
            handleRequest(
                HttpMethod.Get, "/meg?fom=2019-09-09&tom=2019-10-10" +
                        "&a=aktør_id&a=fornavn&a=mellomnavn&a=etternavn&a=fødselsdato" +
                        "&a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn&a=barn[].fødselsdato&a=barn[].har_samme_adresse&a=barn[].identitetsnummer" +
                        "&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn&a=kontonummer"
            ) {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "oppslag-alle-attrib")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
            {
                "aktør_id": "12345",
                "fornavn": "STOR-KAR",
                "mellomnavn": "LANGEMANN",
                "etternavn": "TEST",
                "fødselsdato": "1985-07-27",
                "kontonummer": "96850814136",
                "barn":[
                    {
                        "fornavn": "OLA",
                        "etternavn": "NORDMANN",
                        "fødselsdato": "2012-02-24",
                        "identitetsnummer": "$BARN_TIL_PERSON_1"
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

    @Test
    fun `test oppslag ingen attributter skal returnere tom JSON`() {
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_1_MED_BARN)
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "oppslag-ingen-attrib")
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                {}
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `test oppslag bare ugyldig attributt - bad request`() {
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_1_MED_BARN)
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=ugyldigAttrib") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "oppslag-ugyldig-attrib")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals("application/problem+json; charset=UTF-8", response.contentType().toString())
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
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `test oppslag ugyldige attributt - bad request`() {
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_1_MED_BARN)
        with(engine) {
            handleRequest(HttpMethod.Get, "/meg?a=aktør_id&a=ugyldigattrib&a=fornavn&a=annetugyldigattrib") {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "oppslag-ugyldige-attrib")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals("application/problem+json; charset=UTF-8", response.contentType().toString())
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
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag feil format fom`() {
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_1_MED_BARN)
        with(engine) {
            handleRequest(
                HttpMethod.Get,
                "/meg?fom=2019/02/02&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer"
            ) {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "oppslag-feil-format-fom")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals("application/problem+json; charset=UTF-8", response.contentType().toString())
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
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag feil format tom`() {
        val idToken: String = LoginService.V1_0.generateJwt(PERSON_1_MED_BARN)
        with(engine) {
            handleRequest(
                HttpMethod.Get,
                "/meg?fom=2019-02-02&tom=2019.10.10&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer"
            ) {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, "oppslag-feil-format-tom")
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                assertEquals("application/problem+json; charset=UTF-8", response.contentType().toString())
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
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }
    
    @Test
    fun `Hente personlige foretak for en person som har det`() {
        val idToken: String = LoginService.V1_0.generateJwt("111111111111")
        with(engine) {
            handleRequest(
                HttpMethod.Get,
                MegUrlGenerator.PersonligeForetak
            ) {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, UUID.randomUUID().toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                {
                    "personlige_foretak": [{
                        "organisasjonsform": "ENK",
                        "registreringsdato": "2020-01-01",
                        "organisasjonsnummer": "1"
                    }, {
                        "organisasjonsform": "DA",
                        "registreringsdato": "2020-02-01",
                        "organisasjonsnummer": "2"
                    }, {
                        "opphørsdato": "2020-06-01",
                        "organisasjonsform": "ANS",
                        "registreringsdato": "2020-03-01",
                        "organisasjonsnummer": "3"
                    }]
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `Hente personlige foretak for en person som ikke har det`() {
        val idToken: String = LoginService.V1_0.generateJwt("111111111112")
        with(engine) {
            handleRequest(
                HttpMethod.Get,
                MegUrlGenerator.PersonligeForetak
            ) {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, UUID.randomUUID().toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                {
                    "personlige_foretak": []
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }

    @Test
    fun `Hente personlige foretak for en person som har fler roller i samme foretak`() {
        val idToken: String = LoginService.V1_0.generateJwt("22222222222")
        with(engine) {
            handleRequest(
                HttpMethod.Get,
                MegUrlGenerator.PersonligeForetak
            ) {
                addHeader(HttpHeaders.Authorization, "Bearer $idToken")
                addHeader(HttpHeaders.XCorrelationId, UUID.randomUUID().toString())
            }.apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("application/json; charset=UTF-8", response.contentType().toString())
                val expectedResponse = """
                {
                    "personlige_foretak": [{
                        "organisasjonsform": "ENK",
                        "registreringsdato": "2020-01-02",
                        "organisasjonsnummer": "1"
                    }]
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, response.content!!, true)
            }
        }
    }
}
