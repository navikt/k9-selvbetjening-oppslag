package no.nav.k9

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import java.time.LocalDate.parse
import org.junit.jupiter.api.Assertions.assertFalse
import no.nav.k9.BarnFødselsnummer.BARN_TIL_PERSON_1
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
import no.nav.k9.integrasjon.common.NavHeaders
import no.nav.k9.integrasjon.aareg.erAnsattIPerioden
import no.nav.k9.wiremocks.getArbeidsgiverOgArbeidstakerV2RegisterUrl
import no.nav.k9.wiremocks.getEnhetsregisterUrl
import no.nav.k9.wiremocks.getPdlUrl
import no.nav.k9.wiremocks.k9SelvbetjeningOppslagWireMockServer
import no.nav.k9.wiremocks.stubArbeidsgiverOgArbeidstakerRegisterV2
import no.nav.k9.wiremocks.stubEnhetsRegister
import no.nav.k9.wiremocks.stubPDLRequest
import no.nav.siftilgangskontroll.core.pdl.utils.PdlOperasjon
import no.nav.siftilgangskontroll.pdl.generated.enums.IdentGruppe
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationTest {

    private companion object {
        private const val AUDIENCE = "dev-fss:dusseldorf:k9-selvbetjening-oppslag"

        val wireMockServer: WireMockServer = k9SelvbetjeningOppslagWireMockServer()
            .stubPDLRequest(PdlOperasjon.HENT_PERSON)
            .stubPDLRequest(PdlOperasjon.HENT_PERSON_BOLK)
            .stubPDLRequest(PdlOperasjon.HENT_IDENTER)
            .stubPDLRequest(PdlOperasjon.HENT_IDENTER_BOLK)
            .stubArbeidsgiverOgArbeidstakerRegisterV2()
            .stubEnhetsRegister()

        val mockOAuth2Server = MockOAuth2Server().apply { start() }

        private val privateJwk = RSAKeyGenerator(2048).keyID("test-key").generate().toJSONString()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.rest.retry.initialDelay") { "10" }
            registry.add("nav.register-urls.pdl-url") { wireMockServer.getPdlUrl() }
            registry.add("nav.register-urls.enhetsregister-v1") { wireMockServer.getEnhetsregisterUrl() }
            registry.add("nav.register-urls.arbeidsgiver-og-arbeidstaker-v2") { wireMockServer.getArbeidsgiverOgArbeidstakerV2RegisterUrl() }

            registry.add("no.nav.security.jwt.issuer.tokenx.discoveryurl") { mockOAuth2Server.wellKnownUrl("tokenx").toString() }
            registry.add("no.nav.security.jwt.issuer.tokenx.accepted_audience") { AUDIENCE }
            registry.add("no.nav.security.jwt.issuer.azure.discoveryurl") { mockOAuth2Server.wellKnownUrl("azure").toString() }
            registry.add("no.nav.security.jwt.issuer.azure.accepted_audience") { AUDIENCE }

            listOf("tokenx-pdl-api" to "dev-fss:pdl:pdl-api", "tokenx-aareg" to "dev-fss.arbeidsforhold.aareg-services-nais")
                .forEach { (registrering, audience) ->
                    val prefix = "no.nav.security.jwt.client.registration.$registrering"
                    registry.add("$prefix.token-endpoint-url") { mockOAuth2Server.tokenEndpointUrl("tokenx").toString() }
                    registry.add("$prefix.authentication.client-id") { "k9-selvbetjening-oppslag" }
                    registry.add("$prefix.authentication.client-jwk") { privateJwk }
                    registry.add("$prefix.token-exchange.audience") { audience }
                }
            val azure = "no.nav.security.jwt.client.registration.azure-pdl-api"
            registry.add("$azure.token-endpoint-url") { mockOAuth2Server.tokenEndpointUrl("azure").toString() }
            registry.add("$azure.scope") { "dev-fss.pdl.pdl-api/.default" }
            registry.add("$azure.authentication.client-id") { "k9-selvbetjening-oppslag" }
            registry.add("$azure.authentication.client-jwk") { privateJwk }
        }
    }

    @LocalServerPort
    private var port: Int = 0

    private val client = TestClient { "http://localhost:$port" }

    private fun assertJsonUtf8(response: TestResponse) =
        assertEquals(MediaType("application", "json", Charsets.UTF_8), response.contentType)

    private fun assertProblemJson(response: TestResponse) =
        assertTrue(response.contentType?.isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON) == true, "Content-Type var ${response.contentType}")

    private fun testApplication(block: () -> Unit) = block()

    @Test
    fun `test readiness, liveness og metrics`() {
        app {
            client.get("/health/readiness").apply {
                assertEquals(HttpStatus.OK, status)
            }
            client.get("/health/liveness").apply {
                assertEquals(HttpStatus.OK, status)
            }
            client.get("/metrics").apply {
                assertEquals(HttpStatus.OK, status)
            }
        }
    }

    @Test
    fun `test oppslag uten idToken gir unauthorized`() {
        app {
            client.get("/meg?a=aktør_id") {
                header(X_CORRELATION_ID, "meg-oppslag-uten-id-token")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.UNAUTHORIZED, status)
            }
        }
    }

    @Test
    fun `test oppslag uten XCorrelationId gir BadRequest`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        app {
            client.get("/meg?a=aktør_id") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.BAD_REQUEST, status)
            }
        }
    }

    @Test
    fun `test megOppslag aktoerId`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        app {
            client.get("/meg?a=aktør_id") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "meg-oppslag-aktoer-id")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
                val expectedResponse = """
                { "aktør_id": "12345" }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `test megOppslag med acr idporten-loa-high`() {
        val idToken: String = mockOAuth2Server.hentToken(
            subject = PERSON_1_MED_BARN,
            claims = mapOf("acr" to "idporten-loa-high")
        )
        app {
            client.get("/meg?a=aktør_id") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "meg-oppslag-idporten-loa-high")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
            }
        }
    }

    @Test
    fun `test megOppslag med utilstrekkelig acr gir unauthorized`() {
        val idToken: String = mockOAuth2Server.hentToken(
            subject = PERSON_1_MED_BARN,
            claims = mapOf("acr" to "Level3")
        )
        app {
            client.get("/meg?a=aktør_id") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "meg-oppslag-for-lav-acr")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.UNAUTHORIZED, status)
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

        app {
            client.get("/meg?a=aktør_id") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $azureToken")
                header(X_CORRELATION_ID, "meg-oppslag-aktoer-id")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.UNAUTHORIZED, status)
            }
        }
    }

    @Test
    fun `test systemoppslag uten XCorrelationId gir BadRequest`() {
        val azureToken = mockOAuth2Server.issueToken(
            issuerId = "azure",
            subject = UUID.randomUUID().toString(),
            audience = "dev-fss:dusseldorf:k9-selvbetjening-oppslag",
            claims = mapOf("roles" to "access_as_application")
        ).serialize()

        app {
            client.post("/system/hent-identer") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $azureToken")
                header(HttpHeaders.ACCEPT, "application/json")
                header(HttpHeaders.CONTENT_TYPE, "application/json")
                //language=json
                body(
                    """
                    {
                        "identer": ["$PERSON_1_MED_BARN"],
                        "identGrupper": ["${IdentGruppe.FOLKEREGISTERIDENT}"]
                    }
                """.trimIndent()
                )
            }.apply {
                assertEquals(HttpStatus.BAD_REQUEST, status)
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

        app {
            client.post("/system/hent-identer") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $azureToken")
                header(X_CORRELATION_ID, "systemoppslag-hent-identer")
                header(HttpHeaders.ACCEPT, "application/json")
                header(HttpHeaders.CONTENT_TYPE, "application/json")
                //language=json
                body(
                    """
                    {
                        "identer": ["$PERSON_1_MED_BARN"],
                        "identGrupper": ["${IdentGruppe.FOLKEREGISTERIDENT}"]
                    }
                """.trimIndent()
                )
            }.apply {
                assertEquals(HttpStatus.OK, status)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `systemoppslag ignorerer ukjente felt i body`() {
        val azureToken = mockOAuth2Server.issueToken(
            issuerId = "azure",
            subject = UUID.randomUUID().toString(),
            audience = "dev-fss:dusseldorf:k9-selvbetjening-oppslag",
            claims = mapOf("roles" to "access_as_application")
        ).serialize()

        app {
            client.post("/system/hent-identer") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $azureToken")
                header(X_CORRELATION_ID, "systemoppslag-ukjent-felt")
                header(HttpHeaders.ACCEPT, "application/json")
                header(HttpHeaders.CONTENT_TYPE, "application/json")
                //language=json
                body(
                    """
                    {
                        "ukjentFelt": "verdi",
                        "identer": ["$PERSON_1_MED_BARN"],
                        "identGrupper": ["${IdentGruppe.FOLKEREGISTERIDENT}"]
                    }
                """.trimIndent()
                )
            }.apply {
                assertEquals(HttpStatus.OK, status)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
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

        app {
            client.post("/system/hent-barn") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $azureToken")
                header(X_CORRELATION_ID, "systemoppslag-hent-barn")
                header(HttpHeaders.ACCEPT, "application/json")
                header(HttpHeaders.CONTENT_TYPE, "application/json")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
                //language=json
                body(
                    """
                    {
                        "identer": ["${BarnFødselsnummer.BARN_TIL_PERSON_1}"]
                    }
                """.trimIndent()
                )
            }.apply {
                assertEquals(HttpStatus.OK, status)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
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

        app {
            client.post("/system/hent-barn") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $azureToken")
                header(X_CORRELATION_ID, "systemoppslag-hent-adresebeskyttet-barn")
                header(HttpHeaders.ACCEPT, "application/json")
                header(HttpHeaders.CONTENT_TYPE, "application/json")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
                //language=json
                body(
                    """
                    {
                        "identer": ["${BarnFødselsnummer.SKJERMET_BARN_TIL_PERSON_3}"]
                    }
                """.trimIndent()
                )
            }.apply {
                assertEquals(HttpStatus.OK, status)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `test megOppslag aktør_id og fornavn`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_2_MED_BARN)
        app {
            client.get("/meg?a=aktør_id&a=fornavn") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "meg-oppslag-aktoer-id-fornavn")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
                val expectedResponse = """
                { "aktør_id": "23456",
                 "fornavn": "ARNE"}
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `test megOppslag aktør_id og navn og fødselsdato`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_2_MED_BARN)
        app {
            client.get("/meg?a=aktør_id&a=fornavn&a=mellomnavn&a=etternavn&a=fødselsdato") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "meg-oppslag-aktoer-id-navn-foedselsdato")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
                val expectedResponse = """
                { 
                    "aktør_id": "23456",
                    "fornavn": "ARNE",
                    "mellomnavn": "BJARNE",
                    "etternavn": "CARLSEN",
                    "fødselsdato": "1990-01-02"
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `test megOppslag navn har ikke mellomnavn`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = "01010067894")
        app {
            client.get("/meg?a=fornavn&a=mellomnavn&a=etternavn") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "meg-oppslag-har-ikke-mellomnavn")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
                val expectedResponse = """
                {
                    "fornavn": "CATO",
                    "mellomnavn": "",
                    "etternavn": "NILSEN"
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `gitt oppslag av død person, forvent feil`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = DØD_PERSON)
        app {
            client.get("/meg?a=fornavn&a=mellomnavn&a=etternavn") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "meg-oppslag-dod-person")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(451, status.value())
                assertProblemJson(this)
                //language=json
                val expectedResponse = """
                {
                    "detail": "Policy decision: DENY - Reason: (NAV-bruker er ikke lenger i live AND NAV-bruker er myndig)",
                    "instance": "/meg",
                    "type": "/problem-details/tilgangskontroll-feil",
                    "title": "tilgangskontroll-feil",
                    "status": 451
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `gitt oppslag av person under myndighetsalder (18), forvent 451 Unavailable For Legal Reasons`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_UNDER_MYNDIGHETS_ALDER)
        app {
            client.get("/meg?a=fornavn&a=mellomnavn&a=etternavn") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "meg-oppslag-under-myndighet")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
            assertEquals(451, status.value())
            assertProblemJson(this)
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
            JSONAssert.assertEquals(expectedResponse, body, true)
        }
        }
    }

    @Test
    fun `test barnOppslag aktoerId`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_2_MED_BARN)
        app {
            client.get("/meg?a=barn[].aktør_id") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "barn-oppslag-aktoer-id")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
                val expectedResponse = """
                { 
                    "barn":[
                        {"aktør_id":"65432"}
                    ]
                }
                """.trimIndent()
                JSONAssert.assertEquals(
                    expectedResponse,
                    body,
                    true
                ) //feiler. AktørId for barn blir satt til forelders aktørId
            }
        }
    }

    @Test
    fun `test barnOppslag navn og fødselsdato`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_2_MED_BARN)
        app {
            client.get(
                "/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn&a=barn[].fødselsdato"
            ) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "barn-oppslag-navn-foedselsdato")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `test barnOppslag navn har ikke mellomnavn`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        app {
            client.get("/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "barn-oppslag-har-ikke-mellomnavn")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `gitt barn med strengt fortrolig adresse, forvent tom liste`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_3_MED_SKJERMET_BARN)
        app {
            client.get("/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "barn-oppslag-har-ikke-mellomnavn")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
                val expectedResponse = """
                { 
                    "barn": []
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `gitt død barn, forvent tom liste`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_4_MED_DØD_BARN)
        app {
            client.get("/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "barn-oppslag-har-ikke-mellomnavn")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
                val expectedResponse = """
                { 
                    "barn": []
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `test barnOppslag ingenBarn`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_UTEN_BARN)
        app {
            client.get("/meg?a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "barn-oppslag-ingen-barn")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
                val expectedResponse = """
                { 
                    "barn":[]
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag orgnr`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        app {
            client.get("/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "arbeidsgiver-oppslag-orgnr")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag orgnr og navn`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        app {
            client.get(
                "/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn"
            ) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "arbeidsgiver-oppslag-orgnr-navn")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `Forvent organiasjon uten navn, gitt at navn ikke er funnet`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        app {
            client.get(
                "/arbeidsgivere?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn&org=11111111"
            ) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "arbeidsgiver-oppslag-orgnr-navn")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `Forvent 1 organisasjon med navn, gitt organisasjonsnummer`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        app {
            client.get(
                "/arbeidsgivere?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn&org=981585216"
            ) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "arbeidsgiver-oppslag-orgnr-navn")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `Forvent 2 organisasjoner med navn, gitt organisasjonsnummer`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        app {
            client.get(
                "/arbeidsgivere?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn&org=981585216&org=67564534"
            ) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "arbeidsgiver-oppslag-orgnr-navn")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag orgnr, navn, fom og tom`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        app {
            client.get(
                "/meg?fom=2019-02-02&tom=2023-10-10&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn&a=arbeidsgivere[].organisasjoner[].ansettelsesperiode"
            ) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "arbeidsgiver-oppslag-orgnr-navn")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag med ingen arbeidsgivere`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_UTEN_ARBEIDSGIVER)
        app {
            client.get(
                "/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn" +
                        "&a=private_arbeidsgivere[].offentlig_ident&a=private_arbeidsgivere[].ansettelsesperiode"
            ) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "arbeidsgiver-oppslag-ingen-arbeidsgiver")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
                val expectedResponse = """
            {
                "arbeidsgivere":{
                    "organisasjoner":[],
                    "private_arbeidsgivere":[]
                }
            }
            """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `tester oppslag av private arbeidsgivere`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        app {
            client.get(
                "/meg?a=private_arbeidsgivere[].offentlig_ident&a=private_arbeidsgivere[].ansettelsesperiode"
            ) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "arbeidsgiver-oppslag-private-arbeidsgivere")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `Forventer å kun få unike arbeidsgivere selvom man har flere arbeidsforhold hos en arbeidsgiver`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_MED_FLERE_ARBEIDSFORHOLD_PER_ARBEIDSGIVER)
        app {
            client.get(
                "/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn" +
                        "&a=private_arbeidsgivere[].offentlig_ident&a=private_arbeidsgivere[].ansettelsesperiode"
            ) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "arbeidsgiver-oppslag-arbeidsgivere")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `Forventer flere ansettelsesperioder hos arbeidsgiver`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_MED_FLERE_ARBEIDSFORHOLD_PER_ARBEIDSGIVER)
        app {
            client.get(
                "/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn&a=arbeidsgivere[].organisasjoner[].ansettelsesperiode&inkluderAlleAnsettelsesperioder=true&fom=2014-01-01"
            ) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "arbeidsgiver-oppslag-arbeidsgivere")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }


    @Test
    fun `Teste oppslag av frilans oppdrag`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_MED_FRILANS_OPPDRAG)
        app {
            client.get(
                "/meg?a=frilansoppdrag[]"
            ) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "arbeidsgiver-oppslag-frilans-oppdrag")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `test oppslag alle attributter`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        app {
            client.get(
                "/meg?fom=2019-09-09&tom=2022-10-10" +
                        "&a=aktør_id&a=fornavn&a=mellomnavn&a=etternavn&a=fødselsdato" +
                        "&a=barn[].fornavn&a=barn[].mellomnavn&a=barn[].etternavn&a=barn[].fødselsdato&a=barn[].har_samme_adresse&a=barn[].identitetsnummer" +
                        "&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn" +
                        "&a=private_arbeidsgivere[].offentlig_ident&a=private_arbeidsgivere[].ansettelsesperiode&a=frilansoppdrag[]"
            ) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "oppslag-alle-attrib")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `test oppslag ingen attributter skal returnere tom JSON`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        app {
            client.get("/meg") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "oppslag-ingen-attrib")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                assertJsonUtf8(this)
                val expectedResponse = """
                {}
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `test oppslag bare ugyldig attributt - bad request`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        app {
            client.get("/meg?a=ugyldigAttrib") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "oppslag-ugyldig-attrib")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.BAD_REQUEST, status)
                assertProblemJson(this)
                val expectedResponse = """
                {
                    "detail":"Requesten inneholder ugyldige parametere.",
                    "instance":"/meg",
                    "type":"/problem-details/invalid-request-parameters",
                    "title":"invalid-request-parameters",
                    "violations":[
                        {"parameterName":"a","parameterType":"QUERY","reason":"Er ikke en støttet attributt.","invalidValue":"ugyldigattrib"}
                    ],
                    "status":400
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `test oppslag ugyldige attributt - bad request`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        app {
            client.get("/meg?a=aktør_id&a=ugyldigattrib&a=fornavn&a=annetugyldigattrib") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "oppslag-ugyldige-attrib")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.BAD_REQUEST, status)
                assertProblemJson(this)
                val expectedResponse = """
                {
                    "detail":"Requesten inneholder ugyldige parametere.",
                    "instance":"/meg",
                    "type":"/problem-details/invalid-request-parameters",
                    "title":"invalid-request-parameters",
                    "violations":[
                        {"parameterName":"a","parameterType":"QUERY","reason":"Er ikke en støttet attributt.","invalidValue":"ugyldigattrib"},
                        {"parameterName":"a","parameterType":"QUERY","reason":"Er ikke en støttet attributt.","invalidValue":"annetugyldigattrib"}
                    ],
                    "status":400
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `gitt oppslag av søker under myndighetsalder, forvent 451 Unavailable For Legal Reasons`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_UNDER_MYNDIGHETS_ALDER)
        app {
            client.get("/meg?a=aktør_id") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "oppslag-ugyldige-attrib")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(451, status.value())
                assertProblemJson(this)
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
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag feil format fom`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        app {
            client.get(
                "/meg?fom=2019/02/02&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer"
            ) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "oppslag-feil-format-fom")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.BAD_REQUEST, status)
                assertProblemJson(this)
                val expectedResponse = """
                {
                    "detail":"Requesten inneholder ugyldige parametere.",
                    "instance":"/meg",
                    "type":"/problem-details/invalid-request-parameters",
                    "title":"invalid-request-parameters",
                    "violations":[
                        {"parameterName":"fom","parameterType":"QUERY","reason":"Må være på format yyyy-mm-dd.","invalidValue":"2019/02/02"}
                    ],
                    "status":400
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    @Test
    fun `test arbeidsgiverOppslag feil format tom`() {
        val idToken: String = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        app {
            client.get(
                "/meg?fom=2019-02-02&tom=2019.10.10&a=arbeidsgivere[].organisasjoner[].organisasjonsnummer"
            ) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "oppslag-feil-format-tom")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.BAD_REQUEST, status)
                assertProblemJson(this)
                val expectedResponse = """
                {
                    "detail":"Requesten inneholder ugyldige parametere.",
                    "instance":"/meg",
                    "type":"/problem-details/invalid-request-parameters",
                    "title":"invalid-request-parameters",
                    "violations":[
                        {"parameterName":"tom","parameterType":"QUERY","reason":"Må være på format yyyy-mm-dd.","invalidValue":"2019.10.10"}
                    ],
                    "status":400
                }
                """.trimIndent()
                JSONAssert.assertEquals(expectedResponse, body, true)
            }
        }
    }

    // ---- Karakteriseringstester: låser dagens oppførsel før migrering til Spring Boot ----

    private fun app(block: () -> Unit) = block()

    private fun azureToken(claims: Map<String, String> = mapOf("roles" to "access_as_application")) =
        mockOAuth2Server.issueToken(
            issuerId = "azure",
            subject = UUID.randomUUID().toString(),
            audience = "dev-fss:dusseldorf:k9-selvbetjening-oppslag",
            claims = claims
        ).serialize()

    private val hentIdenterBody = """
        {
            "identer": ["$PERSON_1_MED_BARN"],
            "identGrupper": ["${IdentGruppe.FOLKEREGISTERIDENT}"]
        }
    """.trimIndent()

    @Test
    fun `ukjent path gir 404 og feil metode gir 405`() = app {
        assertEquals(HttpStatus.NOT_FOUND, client.get("/finnes-ikke").status)
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.post("/meg") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            header(X_CORRELATION_ID, "feil-metode-meg")
        }.apply {
            assertEquals(HttpStatus.METHOD_NOT_ALLOWED, status)
        }
    }

    @Test
    fun `meg uten X-K9-Ytelse gir 500`() = app {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get("/meg?a=aktør_id") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            header(X_CORRELATION_ID, "meg-uten-ytelse")
        }.apply {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status)
            assertProblemJson(this)
        }
    }

    @Test
    fun `meg med ugyldig X-K9-Ytelse gir 500`() = app {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get("/meg?a=aktør_id") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            header(X_CORRELATION_ID, "meg-ugyldig-ytelse")
            header(NavHeaders.XK9Ytelse, "IKKE_EN_YTELSE")
        }.apply {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status)
            assertProblemJson(this)
        }
    }

    @Test
    fun `meg uten attributter og uten X-K9-Ytelse gir 500 fordi ytelse leses først`() = app {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get("/meg") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            header(X_CORRELATION_ID, "meg-uten-attributter-uten-ytelse")
        }.apply {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status)
        }
    }

    @Test
    fun `ugyldig format på X-Correlation-ID gir 400`() = app {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        listOf("abc", "har mellomrom", "ugyldig!tegn").forEach { correlationId ->
            client.get("/meg?a=aktør_id") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, correlationId)
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.BAD_REQUEST, status, "Correlation-ID '$correlationId'")
                assertProblemJson(this)
            }
        }
    }

    @Test
    fun `meg uten token og uten correlation-id gir 400 fordi correlation-id sjekkes før autentisering`() = app {
        client.get("/meg?a=aktør_id") {
            header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
        }.apply {
            assertEquals(HttpStatus.BAD_REQUEST, status)
        }
    }

    @Test
    fun `system uten token og uten correlation-id gir 400 fordi correlation-id sjekkes før autentisering`() = app {
        client.post("/system/hent-identer") {
            header(HttpHeaders.CONTENT_TYPE, "application/json")
            body(hentIdenterBody)
        }.apply {
            assertEquals(HttpStatus.BAD_REQUEST, status)
        }
    }

    @Test
    fun `meg med tokenx-token uten acr Level4 gir 401`() = app {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN, claims = mapOf("acr" to "Level3"))
        client.get("/meg?a=aktør_id") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            header(X_CORRELATION_ID, "meg-uten-level4")
            header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
        }.apply {
            assertEquals(HttpStatus.UNAUTHORIZED, status)
        }
        val utenAcr = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN, claims = emptyMap())
        client.get("/meg?a=aktør_id") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $utenAcr")
            header(X_CORRELATION_ID, "meg-uten-acr")
            header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
        }.apply {
            assertEquals(HttpStatus.UNAUTHORIZED, status)
        }
    }

    @Test
    fun `arbeidsgivere med azure-token gir 401`() = app {
        client.get("/arbeidsgivere?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer") {
            header(HttpHeaders.AUTHORIZATION, "Bearer ${azureToken()}")
            header(X_CORRELATION_ID, "arbeidsgivere-azure")
        }.apply {
            assertEquals(HttpStatus.UNAUTHORIZED, status)
        }
    }

    @Test
    fun `system med tokenx-token gir 401`() = app {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.post("/system/hent-identer") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            header(X_CORRELATION_ID, "system-med-tokenx")
            header(HttpHeaders.CONTENT_TYPE, "application/json")
            body(hentIdenterBody)
        }.apply {
            assertEquals(HttpStatus.UNAUTHORIZED, status)
        }
    }

    @Test
    fun `system med azure-token uten rollen access_as_application gir 401`() = app {
        client.post("/system/hent-identer") {
            header(HttpHeaders.AUTHORIZATION, "Bearer ${azureToken(claims = emptyMap())}")
            header(X_CORRELATION_ID, "system-uten-rolle")
            header(HttpHeaders.CONTENT_TYPE, "application/json")
            body(hentIdenterBody)
        }.apply {
            assertEquals(HttpStatus.UNAUTHORIZED, status)
        }
    }

    @Test
    fun `system med ugyldig eller tom body gir 500`() = app {
        listOf("ikke json", "", "{}").forEach { body ->
            client.post("/system/hent-identer") {
                header(HttpHeaders.AUTHORIZATION, "Bearer ${azureToken()}")
                header(X_CORRELATION_ID, "system-ugyldig-body")
                header(HttpHeaders.CONTENT_TYPE, "application/json")
                body(body)
            }.apply {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status, "Body '$body'")
                assertProblemJson(this)
            }
        }
    }

    @Test
    fun `system hent-barn uten X-K9-Ytelse gir 500`() = app {
        client.post("/system/hent-barn") {
            header(HttpHeaders.AUTHORIZATION, "Bearer ${azureToken()}")
            header(X_CORRELATION_ID, "system-hent-barn-uten-ytelse")
            header(HttpHeaders.CONTENT_TYPE, "application/json")
            body("""{ "identer": ["$BARN_TIL_PERSON_1"] }""")
        }.apply {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status)
        }
    }

    @Test
    fun `system ignorerer ukjente felter i request body`() = app {
        client.post("/system/hent-identer") {
            header(HttpHeaders.AUTHORIZATION, "Bearer ${azureToken()}")
            header(X_CORRELATION_ID, "system-ukjent-felt")
            header(HttpHeaders.CONTENT_TYPE, "application/json")
            body(
                """
                {
                    "identer": ["$PERSON_1_MED_BARN"],
                    "identGrupper": ["${IdentGruppe.FOLKEREGISTERIDENT}"],
                    "ukjentFelt": "ignoreres"
                }
                """.trimIndent()
            )
        }.apply {
            assertEquals(HttpStatus.OK, status)
        }
    }

    @Test
    fun `attributter er case-insensitive, blanke filtreres bort og duplikater fjernes`() = app {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get("/meg?a=&a=%20&a=AKTØR_ID&a=aktør_id") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            header(X_CORRELATION_ID, "attributter-case")
            header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
        }.apply {
            assertEquals(HttpStatus.OK, status)
            JSONAssert.assertEquals("""{ "aktør_id": "12345" }""", body, true)
        }
    }

    @Test
    fun `prosentenkodet query gir samme svar som ukodet`() = app {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get("/meg?a=akt%C3%B8r_id&a=arbeidsgivere%5B%5D.organisasjoner%5B%5D.organisasjonsnummer") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            header(X_CORRELATION_ID, "prosentenkodet-query")
            header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
        }.apply {
            assertEquals(HttpStatus.OK, status)
            JSONAssert.assertEquals(
                """{ "aktør_id": "12345", "arbeidsgivere": { "organisasjoner": [ { "organisasjonsnummer": "123456789" } ] } }""",
                body,
                true
            )
        }
    }

    @Test
    fun `arbeidsgivere-endepunktet gir organisasjoner`() = app {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get("/arbeidsgivere?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&org=981585216") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            header(X_CORRELATION_ID, "arbeidsgivere-endepunkt")
        }.apply {
            assertEquals(HttpStatus.OK, status)
            assertJsonUtf8(this)
            JSONAssert.assertEquals(
                """{ "arbeidsgivere": { "organisasjoner": [ { "organisasjonsnummer": "981585216" } ] } }""",
                body,
                true
            )
        }
    }

    @Test
    fun `aareg som svarer 500 gir 500 etter retry`() = app {
        val fnr = "01010010098"
        val stub = wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathMatching("/arbeidsgiver-og-arbeidstaker-register-v2-mock/arbeidstaker/arbeidsforhold.*"))
                .withHeader(NavHeaders.PersonIdent, WireMock.equalTo(fnr))
                .atPriority(1)
                .willReturn(WireMock.aResponse().withStatus(500).withBody("""{"feil":"aareg nede"}"""))
        )
        try {
            val idToken = mockOAuth2Server.hentToken(subject = fnr)
            client.get("/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "aareg-500")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status)
                assertProblemJson(this)
                assertFalse(body.contains("aareg nede"), "Feilmeldingen fra aareg skal ikke lekke ut")
            }
            wireMockServer.verify(
                3,
                WireMock.getRequestedFor(WireMock.urlPathMatching("/arbeidsgiver-og-arbeidstaker-register-v2-mock/arbeidstaker/arbeidsforhold.*"))
                    .withHeader(NavHeaders.PersonIdent, WireMock.equalTo(fnr))
            )
        } finally {
            wireMockServer.removeStub(stub)
        }
    }

    @Test
    fun `aareg som svarer 4xx retryes ikke`() = app {
        val fnr = "01010010099"
        val urlPattern = WireMock.urlPathMatching("/arbeidsgiver-og-arbeidstaker-register-v2-mock/arbeidstaker/arbeidsforhold.*")
        val stub = wireMockServer.stubFor(
            WireMock.get(urlPattern)
                .withHeader(NavHeaders.PersonIdent, WireMock.equalTo(fnr))
                .atPriority(1)
                .willReturn(WireMock.aResponse().withStatus(403))
        )
        try {
            val idToken = mockOAuth2Server.hentToken(subject = fnr)
            client.get("/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "aareg-403")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status)
            }
            wireMockServer.verify(
                1,
                WireMock.getRequestedFor(urlPattern).withHeader(NavHeaders.PersonIdent, WireMock.equalTo(fnr))
            )
        } finally {
            wireMockServer.removeStub(stub)
        }
    }

    @Test
    fun `ereg som svarer 500 gir organisasjon uten navn`() = app {
        val stub = wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathMatching("/enhets-register-mock/organisasjon/123456789/noekkelinfo"))
                .atPriority(1)
                .willReturn(WireMock.aResponse().withStatus(500))
        )
        try {
            val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
            client.get("/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer&a=arbeidsgivere[].organisasjoner[].navn") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "ereg-500")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.OK, status)
                JSONAssert.assertEquals(
                    """{ "arbeidsgivere": { "organisasjoner": [ { "organisasjonsnummer": "123456789" } ] } }""",
                    body,
                    true
                )
            }
        } finally {
            wireMockServer.removeStub(stub)
        }
    }

    @Test
    fun `PDL som returnerer errors gir 500`() = app {
        val fnr = "01010010097"
        val stub = wireMockServer.stubFor(
            WireMock.post(WireMock.urlPathMatching("/graphql"))
                .withRequestBody(WireMock.matchingJsonPath("$.variables.ident", WireMock.equalTo(fnr)))
                .atPriority(1)
                .willReturn(
                    WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("""{ "errors": [ { "message": "pdl feil" } ], "data": null }""")
                )
        )
        try {
            val idToken = mockOAuth2Server.hentToken(subject = fnr)
            client.get("/meg?a=aktør_id") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
                header(X_CORRELATION_ID, "pdl-errors")
                header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
            }.apply {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, status)
                assertFalse(body.contains("pdl feil"), "Feilmeldingen fra PDL skal ikke lekke ut")
            }
        } finally {
            wireMockServer.removeStub(stub)
        }
    }

    @Test
    fun `utgående kall til PDL bærer Nav-Call-Id fra X-Correlation-ID`() = app {
        wireMockServer.resetRequests()
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get("/meg?a=aktør_id") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            header(X_CORRELATION_ID, "call-id-propagering")
            header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
        }.apply { assertEquals(HttpStatus.OK, status) }
        wireMockServer.verify(
            WireMock.postRequestedFor(WireMock.urlPathMatching("/graphql"))
                .withHeader(NavHeaders.CallId, WireMock.equalTo("call-id-propagering"))
                .withHeader(NavHeaders.Tema, WireMock.equalTo("OMS"))
        )
    }

    @Test
    fun `rå hakeparenteser i query uten prosent-enkoding godtas`() = app {
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        val (statusLinje, body) = rawGet(
            "/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer",
            "Authorization" to "Bearer $idToken",
            X_CORRELATION_ID to "raa-query-tegn",
            NavHeaders.XK9Ytelse to "${Ytelse.PLEIEPENGER_SYKT_BARN}"
        )
        assertTrue(statusLinje.contains(" 200"), "Statuslinje var $statusLinje")
        JSONAssert.assertEquals(
            """{"arbeidsgivere":{"organisasjoner":[{"organisasjonsnummer":"123456789"}]}}""",
            body,
            true
        )
    }

    @Test
    fun `token-exchange mot PDL bruker subject-token fra requesten`() = app {
        wireMockServer.resetRequests()
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get("/meg?a=aktør_id") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            header(X_CORRELATION_ID, "subject-token-pdl")
            header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
        }.apply { assertEquals(HttpStatus.OK, status) }

        val autorisering = wireMockServer.findAll(WireMock.postRequestedFor(WireMock.urlPathMatching("/graphql")))
            .map { it.getHeader(HttpHeaders.AUTHORIZATION) }
        assertTrue(autorisering.isNotEmpty())
        autorisering.forEach { header ->
            val claims = com.nimbusds.jwt.SignedJWT.parse(header.removePrefix("Bearer ")).jwtClaimsSet
            assertEquals(PERSON_1_MED_BARN, claims.subject)
            assertEquals(listOf("dev-fss:pdl:pdl-api"), claims.audience)
        }
    }

    @Test
    fun `utgående kall til aareg bærer Nav-Call-Id og token-exchange-token`() = app {
        wireMockServer.resetRequests()
        val idToken = mockOAuth2Server.hentToken(subject = PERSON_1_MED_BARN)
        client.get("/meg?a=arbeidsgivere[].organisasjoner[].organisasjonsnummer") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $idToken")
            header(X_CORRELATION_ID, "call-id-aareg")
            header(NavHeaders.XK9Ytelse, "${Ytelse.PLEIEPENGER_SYKT_BARN}")
        }.apply { assertEquals(HttpStatus.OK, status) }

        val kall = wireMockServer.findAll(
            WireMock.getRequestedFor(WireMock.urlPathMatching("/arbeidsgiver-og-arbeidstaker-register-v2-mock/arbeidstaker/arbeidsforhold.*"))
        )
        assertEquals(1, kall.size)
        assertEquals("call-id-aareg", kall.single().getHeader(NavHeaders.CallId))
        assertEquals(PERSON_1_MED_BARN, kall.single().getHeader(NavHeaders.PersonIdent))
        val claims = com.nimbusds.jwt.SignedJWT.parse(kall.single().getHeader(HttpHeaders.AUTHORIZATION).removePrefix("Bearer ")).jwtClaimsSet
        assertEquals(listOf("dev-fss.arbeidsforhold.aareg-services-nais"), claims.audience)
    }

    private fun rawGet(pathAndQuery: String, vararg headers: Pair<String, String>): Pair<String, String> =
        java.net.Socket("localhost", port).use { socket ->
            val forespørsel = buildString {
                append("GET $pathAndQuery HTTP/1.1\r\nHost: localhost:$port\r\nConnection: close\r\n")
                headers.forEach { (navn, verdi) -> append("$navn: $verdi\r\n") }
                append("\r\n")
            }
            socket.getOutputStream().apply { write(forespørsel.toByteArray(Charsets.UTF_8)); flush() }
            val svar = socket.getInputStream().readBytes().toString(Charsets.UTF_8)
            val hode = svar.substringBefore("\r\n\r\n")
            val kropp = svar.substringAfter("\r\n\r\n")
            val erChunked = hode.lines().any { it.equals("Transfer-Encoding: chunked", ignoreCase = true) }
            svar.substringBefore("\r\n") to if (erChunked) kropp.dekodChunked() else kropp
        }

    private fun String.dekodChunked(): String = buildString {
        var rest = this@dekodChunked
        while (true) {
            val lengde = rest.substringBefore("\r\n").trim().toInt(16)
            if (lengde == 0) break
            rest = rest.substringAfter("\r\n")
            append(rest.substring(0, lengde))
            rest = rest.substring(lengde).removePrefix("\r\n")
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
