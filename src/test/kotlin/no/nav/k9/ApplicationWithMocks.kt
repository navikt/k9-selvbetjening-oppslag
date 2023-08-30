package no.nav.k9

import io.ktor.server.testing.*
import no.nav.helse.dusseldorf.testsupport.asArguments
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.k9.wiremocks.*
import no.nav.k9.wiremocks.k9SelvbetjeningOppslagConfig
import no.nav.k9.wiremocks.stubEnhetsRegister
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ApplicationWithMocks {
    companion object {

        private val logger: Logger = LoggerFactory.getLogger(ApplicationWithMocks::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            val mockOAuth2Server = MockOAuth2Server().apply { start() }

            val wireMockServer = WireMockBuilder()
                .withPort(8081)
                .k9SelvbetjeningOppslagConfig()
                .build()
                .stubArbeidsgiverOgArbeidstakerRegisterV2()
                .stubEnhetsRegister()

            val testArgs = TestConfiguration.asMap(
                wireMockServer = wireMockServer,
                mockOAuth2Server = mockOAuth2Server
            ).asArguments()

            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    logger.info("Tearing down")
                    wireMockServer.stop()
                    logger.info("Tear down complete")
                }
            })

            testApplication { no.nav.k9.main(testArgs) }
        }
    }
}
