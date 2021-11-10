package no.nav.k9

import com.github.kittinunf.fuel.httpGet
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.helse.dusseldorf.testsupport.jws.ClientCredentials
import no.nav.helse.dusseldorf.testsupport.wiremock.getAzureV2WellKnownUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getLoginServiceV1WellKnownUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsWellKnownUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getTokendingsWellKnownUrl
import no.nav.k9.wiremocks.*
import org.json.JSONObject

object TestConfiguration {

    fun asMap(
        wireMockServer: WireMockServer? = null,
        port : Int = 8080,
        arbeidsgiverOgArbeidstakerRegisterBaseUrl : String? = wireMockServer?.getArbeidsgiverOgArbeidstakerRegisterUrl(),
        enhetsRegisterBaseUrl : String? = wireMockServer?.getEnhetsregisterUrl(),
        brregProxyV1BaseUrl : String? = wireMockServer?.getBrregProxyV1BaseUrl(),
        tpsProxyBaseUrl : String? = wireMockServer?.getTpsProxyUrl(),
        pdlUrl : String? = wireMockServer?.getPdlUrl()
    ) : Map<String, String> {
        val naisStsWellKnownJson = wireMockServer?.getNaisStsWellKnownUrl()?.getAsJson()

        val map = mutableMapOf(
            Pair("ktor.deployment.port","$port"),

            Pair("nav.register_urls.tps_proxy_v1", "$tpsProxyBaseUrl"),
            Pair("nav.register_urls.arbeidsgiver_og_arbeidstaker_v1", "$arbeidsgiverOgArbeidstakerRegisterBaseUrl"),
            Pair("nav.register_urls.enhetsregister_v1", "$enhetsRegisterBaseUrl"),
            Pair("nav.register_urls.brreg_proxy_v1", "$brregProxyV1BaseUrl"),
            Pair("nav.register_urls.pdl_url", "$pdlUrl"),

            Pair("nav.auth.rest_token_url", "${naisStsWellKnownJson?.getString("token_endpoint")}"),
            Pair("nav.auth.client_id", "k9-selvbetjening-oppslag"),
            Pair("nav.auth.client_secret", "mySecret"),
            Pair("nav.auth.pdl_api_tokenx_audience", "dev-fss:pdl:pdl-api"),
            Pair("nav.auth.pdl_api_azure_audience", "dev-fss.pdl.pdl-api/.default"),

            Pair("nav.auth.issuers.0.alias", "login-service-v1"),
            Pair("nav.auth.issuers.0.discovery_endpoint", wireMockServer!!.getLoginServiceV1WellKnownUrl()),

            // Clients
            Pair("nav.auth.clients.0.alias", "tokenx-pdl-api"),
            Pair("nav.auth.clients.0.client_id", "k9-selvbetjening-oppslag"),
            Pair("nav.auth.clients.0.private_key_jwk", ClientCredentials.ClientC.privateKeyJwk),
            Pair("nav.auth.clients.0.discovery_endpoint", wireMockServer.getTokendingsWellKnownUrl()),
            Pair("nav.auth.clients.1.alias", "azure-pdl-api"),
            Pair("nav.auth.clients.1.client_id", "k9-selvbetjening-oppslag"),
            Pair("nav.auth.clients.1.private_key_jwk", ClientCredentials.ClientA.privateKeyJwk),
            Pair("nav.auth.clients.1.discovery_endpoint", wireMockServer.getAzureV2WellKnownUrl()),
        )
        return map.toMap()
    }

    private fun String.getAsJson() = JSONObject(this.httpGet().responseString().third.component1())
}
