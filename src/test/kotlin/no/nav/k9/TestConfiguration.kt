package no.nav.k9

import com.github.kittinunf.fuel.httpGet
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.helse.dusseldorf.testsupport.jws.ClientCredentials
import no.nav.helse.dusseldorf.testsupport.wiremock.*
import no.nav.k9.wiremocks.*
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.json.JSONObject

object TestConfiguration {

    fun asMap(
        wireMockServer: WireMockServer? = null,
        mockOAuth2Server: MockOAuth2Server? = null,
        port : Int = 8080,
        arbeidsgiverOgArbeidstakerRegisterV2BaseUrl : String? = wireMockServer?.getArbeidsgiverOgArbeidstakerV2RegisterUrl(),
        enhetsRegisterBaseUrl : String? = wireMockServer?.getEnhetsregisterUrl(),
        pdlUrl : String? = wireMockServer?.getPdlUrl()
    ) : Map<String, String> {

        val map = mutableMapOf(
            Pair("ktor.deployment.port","$port"),

            Pair("nav.register_urls.arbeidsgiver_og_arbeidstaker_v2", "$arbeidsgiverOgArbeidstakerRegisterV2BaseUrl"),
            Pair("nav.register_urls.enhetsregister_v1", "$enhetsRegisterBaseUrl"),
            Pair("nav.register_urls.pdl_url", "$pdlUrl"),

            Pair("nav.auth.pdl_api_tokenx_audience", "dev-fss:pdl:pdl-api"),
            Pair("nav.auth.pdl_api_azure_audience", "dev-fss.pdl.pdl-api/.default"),
            Pair("nav.auth.aareg_tokenx_audience", "dev-fss.arbeidsforhold.aareg-services-nais"),

            Pair("no.nav.security.jwt.issuers.0.issuer_name", "tokendings"),
            Pair("no.nav.security.jwt.issuers.0.discoveryurl", "${mockOAuth2Server!!.wellKnownUrl("tokendings")}"),
            Pair("no.nav.security.jwt.issuers.0.accepted_audience", "dev-fss:dusseldorf:k9-selvbetjening-oppslag"),

            Pair("no.nav.security.jwt.issuers.1.issuer_name", "azure"),
            Pair("no.nav.security.jwt.issuers.1.discoveryurl", "${mockOAuth2Server.wellKnownUrl("azure")}"),
            Pair("no.nav.security.jwt.issuers.1.accepted_audience", "dev-fss:dusseldorf:k9-selvbetjening-oppslag"),

            // Clients
            Pair("nav.auth.clients.0.alias", "tokenx"),
            Pair("nav.auth.clients.0.client_id", "k9-selvbetjening-oppslag"),
            Pair("nav.auth.clients.0.private_key_jwk", ClientCredentials.ClientC.privateKeyJwk),
            Pair("nav.auth.clients.0.discovery_endpoint", wireMockServer!!.getTokendingsWellKnownUrl()),
            Pair("nav.auth.clients.1.alias", "azure"),
            Pair("nav.auth.clients.1.client_id", "k9-selvbetjening-oppslag"),
            Pair("nav.auth.clients.1.private_key_jwk", ClientCredentials.ClientA.privateKeyJwk),
            Pair("nav.auth.clients.1.discovery_endpoint", wireMockServer.getAzureV2WellKnownUrl()),
        )
        return map.toMap()
    }

    private fun String.getAsJson() = JSONObject(this.httpGet().responseString().third.component1())
}
