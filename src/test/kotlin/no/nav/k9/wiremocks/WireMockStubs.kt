package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import io.ktor.http.HttpHeaders
import no.nav.helse.dusseldorf.testsupport.wiremock.WireMockBuilder
import no.nav.k9.utgaende.rest.NavHeaders
import no.nav.siftilgangskontroll.core.pdl.utils.PdlOperasjon

private const val arbeidsgiverOgArbeidstakerRegisterV1ServerPath = "/arbeidsgiver-og-arbeidstaker-register-v1-mock"
private const val arbeidsgiverOgArbeidstakerRegisterV2ServerPath = "/arbeidsgiver-og-arbeidstaker-register-v2-mock"
private const val enhetsRegisterServerPath = "/enhets-register-mock"
private const val pdlServerPath = "/graphql"

internal fun WireMockBuilder.k9SelvbetjeningOppslagConfig() = wireMockConfiguration {
    it
        .extensions(PdlAktoerIdResponseTransformer())
        .extensions(PDLHentPersonBolkResponseTransformer())
        .extensions(PDLPersonResponseTransformer())
        .extensions(PDLHentIdentBolkResponseTransformer())
        .extensions(ArbeidstakerResponseTransformer())
        .extensions(ArbeidstakerResponseV2Transformer())
        .extensions(EnhetsregResponseTransformer())
        .extensions(BrregProxyV1ResponseTransformer())
}

internal fun WireMockServer.stubPDLRequest(pdlOperasjon: PdlOperasjon): WireMockServer {
    WireMock.stubFor(
        WireMock.post(WireMock.urlPathMatching(pdlServerPath))
            .withHeader(NavHeaders.ConsumerToken, AnythingPattern())
            .withHeader(HttpHeaders.Authorization, AnythingPattern())
            .withHeader(NavHeaders.CallId, AnythingPattern())
            .withHeader(NavHeaders.Tema, EqualToPattern("OMS"))
            .withRequestBody(matchingJsonPath("$.query", containing(pdlOperasjon.navn)))
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withTransformers(when (pdlOperasjon) {
                        PdlOperasjon.HENT_PERSON -> "pdl-hent-person"
                        PdlOperasjon.HENT_PERSON_BOLK -> "pdl-hent-barn"
                        PdlOperasjon.HENT_IDENTER -> "pdl-hent-ident"
                        PdlOperasjon.HENT_IDENTER_BOLK -> "pdl-hent-identer-bolk"
                    })
            )
    )
    return this
}

internal fun WireMockServer.stubArbeidsgiverOgArbeidstakerRegister(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching("$arbeidsgiverOgArbeidstakerRegisterV1ServerPath/arbeidstaker/arbeidsforhold*"))
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

internal fun WireMockServer.stubArbeidsgiverOgArbeidstakerRegisterV2(): WireMockServer {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching("$arbeidsgiverOgArbeidstakerRegisterV2ServerPath/arbeidstaker/arbeidsforhold*"))
            .withHeader(HttpHeaders.Authorization, AnythingPattern())
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withTransformers("arbeidstaker-arbeidsforhold-v2")
            )
    )
    return this
}

internal fun WireMockServer.stubEnhetsRegister(): WireMockServer {
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


internal fun WireMockServer.getArbeidsgiverOgArbeidstakerV1RegisterUrl() = baseUrl() + arbeidsgiverOgArbeidstakerRegisterV1ServerPath
internal fun WireMockServer.getArbeidsgiverOgArbeidstakerV2RegisterUrl() = baseUrl() + arbeidsgiverOgArbeidstakerRegisterV2ServerPath

internal fun WireMockServer.getEnhetsregisterUrl() = baseUrl() + enhetsRegisterServerPath
internal fun WireMockServer.getPdlUrl() = baseUrl() + pdlServerPath
