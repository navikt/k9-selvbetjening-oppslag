package no.nav.k9.wiremocks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import no.nav.k9.integrasjon.common.NavHeaders
import no.nav.siftilgangskontroll.core.behandling.Behandling
import no.nav.siftilgangskontroll.core.pdl.utils.PdlOperasjon

private const val arbeidsgiverOgArbeidstakerRegisterV2ServerPath = "/arbeidsgiver-og-arbeidstaker-register-v2-mock"
private const val enhetsRegisterServerPath = "/enhets-register-mock"
private const val pdlServerPath = "/graphql"
private const val AUTHORIZATION = "Authorization"

internal fun WireMockServer.stubPDLRequest(pdlOperasjon: PdlOperasjon): WireMockServer {
    var behandlingsnummer = ""
    Behandling.entries.forEach {
        behandlingsnummer += "${it.behandlingsnummer}|"
    }
    val requestBuilder = WireMock.post(WireMock.urlPathMatching(pdlServerPath))
        .withHeader(NavHeaders.ConsumerToken, AnythingPattern())
        .withHeader(AUTHORIZATION, AnythingPattern())
        .withHeader(NavHeaders.CallId, AnythingPattern())
        .withHeader(NavHeaders.Tema, EqualToPattern("OMS"))
        .withRequestBody(matchingJsonPath("$.query", containing(pdlOperasjon.navn)))

    when (pdlOperasjon) {
        PdlOperasjon.HENT_PERSON_BOLK, PdlOperasjon.HENT_PERSON -> {
            requestBuilder.withHeader(NavHeaders.Behandlingsnummer, WireMock.matching(behandlingsnummer))
        }

        else -> {}
    }

    stubFor(
        requestBuilder
            .willReturn(
                WireMock.aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withStatus(200)
                    .withTransformers(
                        when (pdlOperasjon) {
                            PdlOperasjon.HENT_PERSON -> "pdl-hent-person"
                            PdlOperasjon.HENT_PERSON_BOLK -> "pdl-hent-barn"
                            PdlOperasjon.HENT_IDENTER -> "pdl-hent-ident"
                            PdlOperasjon.HENT_IDENTER_BOLK -> "pdl-hent-identer-bolk"
                        }
                    )
            )
    )
    return this
}

internal fun WireMockServer.stubArbeidsgiverOgArbeidstakerRegisterV2(): WireMockServer {
    stubFor(
        WireMock.get(WireMock.urlPathMatching("$arbeidsgiverOgArbeidstakerRegisterV2ServerPath/arbeidstaker/arbeidsforhold*"))
            .withHeader(AUTHORIZATION, AnythingPattern())
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
    stubFor(
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
