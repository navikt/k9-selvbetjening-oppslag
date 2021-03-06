package no.nav.k9

import io.ktor.server.testing.withApplication
import no.nav.helse.dusseldorf.testsupport.asArguments
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.k9.wiremocks.*
import no.nav.k9.wiremocks.k9SelvbetjeningOppslagConfig
import no.nav.k9.wiremocks.stubAktoerRegisterGetAktoerId
import no.nav.k9.wiremocks.stubArbeidsgiverOgArbeidstakerRegister
import no.nav.k9.wiremocks.stubEnhetsRegister
import no.nav.k9.wiremocks.stubTpsProxyGetBarn
import no.nav.k9.wiremocks.stubTpsProxyGetNavn
import no.nav.k9.wiremocks.stubTpsProxyGetPerson
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ApplicationWithMocks {
    companion object {

        private val logger: Logger = LoggerFactory.getLogger(ApplicationWithMocks::class.java)

        @JvmStatic
        fun main(args: Array<String>) {

            val wireMockServer = WireMockBuilder()
                .withPort(8081)
                .withNaisStsSupport()
                .withLoginServiceSupport()
                .k9SelvbetjeningOppslagConfig()
                .build()
                .stubAktoerRegisterGetAktoerId()
                .stubTpsProxyGetPerson()
                .stubTpsProxyGetBarn()
                .stubArbeidsgiverOgArbeidstakerRegister()
                .stubEnhetsRegister()
                .stubTpsProxyGetNavn()
                .stubBrregProxyV1()

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
