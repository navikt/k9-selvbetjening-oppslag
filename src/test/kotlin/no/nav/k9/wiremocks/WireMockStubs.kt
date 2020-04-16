package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import io.ktor.http.HttpHeaders
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder

private const val aktoerRegisterServerPath = "/aktoer-register-mock"
private const val arbeidsgiverOgArbeidstakerRegisterServerPath = "/arbeidsgiver-og-arbeidstaker-register-mock"
private const val enhetsRegisterServerPath = "/enhets-register-mock"
private const val tpsProxyServerPath = "/tps-proxy-mock"
private const val brregProxyV1ServerPath = "/brreg-proxy-v1-mock"

internal fun WireMockBuilder.k9SelvbetjeningOppslagConfig() = wireMockConfiguration {
    it
        .extensions(AktoerRegisterResponseTransformer())
        .extensions(TpsProxyResponseTransformer())
        .extensions(TpsProxyBarnResponseTransformer())
        .extensions(ArbeidstakerResponseTransformer())
        .extensions(EnhetsregResponseTransformer())
}

internal fun WireMockServer.stubAktoerRegisterGetAktoerId() : WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching("$aktoerRegisterServerPath/.*"))
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

internal fun WireMockServer.stubTpsProxyGetPerson() : WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching("$tpsProxyServerPath/innsyn/person*"))
            .withHeader(HttpHeaders.Authorization, AnythingPattern())
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withTransformers("tps-proxy-person")
            )
    )
    return this
}

internal fun WireMockServer.stubTpsProxyGetNavn() : WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching("$tpsProxyServerPath/navn"))
            .withHeader(HttpHeaders.Authorization, AnythingPattern())
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withBody(
                        """  
                            {
                                "fornavn": "KLØKTIG",
                                "mellomnavn": "BLUNKENDE",
                                "etternavn": "SUPERKONSOLL"
                            }
                        """.trimIndent()
                    )
            )
    )
    return this
}

internal fun WireMockServer.stubTpsProxyGetBarn() : WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching("$tpsProxyServerPath/innsyn/barn*"))
            .withHeader(HttpHeaders.Authorization, AnythingPattern())
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withTransformers("tps-proxy-barn")
            )
    )
    return this
}

internal fun WireMockServer.stubArbeidsgiverOgArbeidstakerRegister() : WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching("$arbeidsgiverOgArbeidstakerRegisterServerPath/arbeidstaker/arbeidsforhold*"))
            .withHeader(HttpHeaders.Authorization, AnythingPattern())
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withTransformers("arbeidstaker-arbeidsforhold")
            )
    )
    return this
}

internal fun WireMockServer.stubEnhetsRegister() : WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching("$enhetsRegisterServerPath/organisasjon/([0-9]*)/noekkelinfo")) // organisasjon/{orgnummer}/noekkelinfo
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withTransformers("enhetsreg-noekkelinfo")
            )
    )
    return this
}

internal fun WireMockServer.getAktoerRegisterUrl() = baseUrl() + aktoerRegisterServerPath
internal fun WireMockServer.getArbeidsgiverOgArbeidstakerRegisterUrl() = baseUrl() + arbeidsgiverOgArbeidstakerRegisterServerPath
internal fun WireMockServer.getEnhetsregisterUrl() = baseUrl() + enhetsRegisterServerPath
internal fun WireMockServer.getTpsProxyUrl() = baseUrl() + tpsProxyServerPath
internal fun WireMockServer.getBrregProxyV1BaseUrl() = baseUrl() + brregProxyV1ServerPath
