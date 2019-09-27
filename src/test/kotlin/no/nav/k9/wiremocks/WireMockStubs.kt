package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import io.ktor.http.HttpHeaders
import no.nav.helse.dusseldorf.ktor.testsupport.wiremock.WireMockBuilder

private const val aktoerRegisterServerPath = "/k9-reverse-proxy/aktoer-register-mock"
private const val arbeidsgiverOgArbeidstakerRegisterServerPath = "/k9-reverse-proxy/arbeidsgiver-og-arbeidstaker-register-mock"
private const val enhetsRegisterServerPath = "/k9-reverse-proxy/enhets-register-mock"
private const val tpsProxyServerPath = "/k9-reverse-proxy/tps-proxy-mock"

internal fun WireMockBuilder.k9SelvbetjeningOppslagApiConfig() = wireMockConfiguration {
    it
        .extensions(AktoerRegisterResponseTransformer())
}

internal fun WireMockServer.stubAktoerRegisterGetAktoerId() : WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching("$aktoerRegisterServerPath/.*"))
            .withHeader("x-nav-apiKey", AnythingPattern())
            .withHeader(HttpHeaders.Authorization, AnythingPattern())
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withTransformers("aktoer-register")
            )
    )
    return this
}

internal fun WireMockServer.getAktoerRegisterUrl() = baseUrl() + aktoerRegisterServerPath
internal fun WireMockServer.getArbeidsgiverOgArbeidstakerRegisterUrl() = baseUrl() + arbeidsgiverOgArbeidstakerRegisterServerPath
internal fun WireMockServer.getEnhetsregisterUrl() = baseUrl() + enhetsRegisterServerPath
internal fun WireMockServer.getTpsProxyUrl() = baseUrl() + tpsProxyServerPath
