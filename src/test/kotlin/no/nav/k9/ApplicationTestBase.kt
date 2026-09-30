package no.nav.k9

import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.k9.PersonFødselsnummer.PERSON_1_MED_BARN
import no.nav.k9.wiremocks.ArbeidstakerResponseTransformer
import no.nav.k9.wiremocks.ArbeidstakerResponseV2Transformer
import no.nav.k9.wiremocks.BrregProxyV1ResponseTransformer
import no.nav.k9.wiremocks.EnhetsregResponseTransformer
import no.nav.k9.wiremocks.PDLHentIdentBolkResponseTransformer
import no.nav.k9.wiremocks.PDLHentPersonBolkResponseTransformer
import no.nav.k9.wiremocks.PDLPersonResponseTransformer
import no.nav.k9.wiremocks.PdlAktoerIdResponseTransformer
import no.nav.k9.wiremocks.stubArbeidsgiverOgArbeidstakerRegisterV2
import no.nav.k9.wiremocks.stubEnhetsRegister
import no.nav.k9.wiremocks.stubPDLRequest
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.siftilgangskontroll.core.pdl.utils.PdlOperasjon
import no.nav.siftilgangskontroll.pdl.generated.enums.IdentGruppe
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.client.RestTestClient
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import org.wiremock.spring.InjectWireMock
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock(
    ConfigureWireMock(
        extensions = [
            PdlAktoerIdResponseTransformer::class,
            PDLHentPersonBolkResponseTransformer::class,
            PDLPersonResponseTransformer::class,
            PDLHentIdentBolkResponseTransformer::class,
            ArbeidstakerResponseTransformer::class,
            ArbeidstakerResponseV2Transformer::class,
            EnhetsregResponseTransformer::class,
            BrregProxyV1ResponseTransformer::class
        ]
    )
)
@EnableMockOAuth2Server
@AutoConfigureRestTestClient
@ActiveProfiles("test")
abstract class ApplicationTestBase {

    protected companion object {
        const val X_CORRELATION_ID = "X-Correlation-ID"
        val JSON_UTF8 = MediaType("application", "json", Charsets.UTF_8)
    }

    @Autowired
    protected lateinit var mockOAuth2Server: MockOAuth2Server

    @InjectWireMock
    protected lateinit var wireMockServer: WireMockServer

    @Autowired
    protected lateinit var client: RestTestClient

    @LocalServerPort
    protected var port: Int = 0

    @BeforeEach
    fun stubRegistre() {
        wireMockServer
            .stubPDLRequest(PdlOperasjon.HENT_PERSON)
            .stubPDLRequest(PdlOperasjon.HENT_PERSON_BOLK)
            .stubPDLRequest(PdlOperasjon.HENT_IDENTER)
            .stubPDLRequest(PdlOperasjon.HENT_IDENTER_BOLK)
            .stubArbeidsgiverOgArbeidstakerRegisterV2()
            .stubEnhetsRegister()
    }

    protected fun azureToken(claims: Map<String, String> = mapOf("roles" to "access_as_application")) =
        mockOAuth2Server.issueToken(
            issuerId = "azure",
            subject = UUID.randomUUID().toString(),
            audience = "dev-fss:dusseldorf:k9-selvbetjening-oppslag",
            claims = claims
        ).serialize()

    protected val hentIdenterBody = """
        {
            "identer": ["$PERSON_1_MED_BARN"],
            "identGrupper": ["${IdentGruppe.FOLKEREGISTERIDENT}"]
        }
    """.trimIndent()
}
