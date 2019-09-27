package no.nav.k9

import io.ktor.server.testing.withApplication
import no.nav.helse.dusseldorf.ktor.testsupport.asArguments
import no.nav.helse.dusseldorf.ktor.testsupport.wiremock.WireMockBuilder
import no.nav.k9.wiremocks.k9SelvbetjeningOppslagApiConfig
import no.nav.k9.wiremocks.stubAktoerRegisterGetAktoerId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ApplicationWithMocks {
    companion object {

        private val logger: Logger = LoggerFactory.getLogger(ApplicationWithMocks::class.java)

        @JvmStatic
        fun main(args: Array<String>) {

            val wireMockServer = WireMockBuilder()
                .withPort(8081)
                .withAzureSupport()
                .withNaisStsSupport()
                .withLoginServiceSupport()
                .k9SelvbetjeningOppslagApiConfig()
                .build()
                .stubAktoerRegisterGetAktoerId()

            val testArgs = TestConfiguration.asMap(wireMockServer = wireMockServer).asArguments()

            Runtime.getRuntime().addShutdownHook(object : Thread() {
                override fun run() {
                    logger.info("Tearing down")
                    wireMockServer.stop()
                    logger.info("Tear down complete")
                }
            })

            withApplication { no.nav.k9.main(testArgs) }
        }
    }
}
