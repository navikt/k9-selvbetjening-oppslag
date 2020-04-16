package no.nav.k9

import com.github.kittinunf.fuel.httpGet
import com.github.tomakehurst.wiremock.WireMockServer
import no.nav.helse.dusseldorf.testsupport.wiremock.getLoginServiceV1WellKnownUrl
import no.nav.helse.dusseldorf.testsupport.wiremock.getNaisStsWellKnownUrl
import no.nav.k9.wiremocks.*
import no.nav.k9.wiremocks.getAktoerRegisterUrl
import no.nav.k9.wiremocks.getArbeidsgiverOgArbeidstakerRegisterUrl
import no.nav.k9.wiremocks.getEnhetsregisterUrl
import no.nav.k9.wiremocks.getTpsProxyUrl
import org.json.JSONObject

object TestConfiguration {

    fun asMap(
        wireMockServer: WireMockServer? = null,
        port : Int = 8080,
        aktoerRegisterBaseUrl : String? = wireMockServer?.getAktoerRegisterUrl(),
        arbeidsgiverOgArbeidstakerRegisterBaseUrl : String? = wireMockServer?.getArbeidsgiverOgArbeidstakerRegisterUrl(),
        enhetsRegisterBaseUrl : String? = wireMockServer?.getEnhetsregisterUrl(),
        brregProxyV1BaseUrl : String? = wireMockServer?.getBrregProxyV1BaseUrl(),
        tpsProxyBaseUrl : String? = wireMockServer?.getTpsProxyUrl()
    ) : Map<String, String> {
        val naisStsWellKnownJson = wireMockServer?.getNaisStsWellKnownUrl()?.getAsJson()

        val map = mutableMapOf(
            Pair("ktor.deployment.port","$port"),

            Pair("nav.register_urls.tps_proxy_v1", "$tpsProxyBaseUrl"),
            Pair("nav.register_urls.aktoer_v1", "$aktoerRegisterBaseUrl"),
            Pair("nav.register_urls.arbeidsgiver_og_arbeidstaker_v1", "$arbeidsgiverOgArbeidstakerRegisterBaseUrl"),
            Pair("nav.register_urls.enhetsregister_v1", "$enhetsRegisterBaseUrl"),
            Pair("nav.register_urls.brreg_proxy_v1", "$brregProxyV1BaseUrl"),

            Pair("nav.auth.rest_token_url", "${naisStsWellKnownJson?.getString("token_endpoint")}"),
            Pair("nav.auth.client_id", "k9-selvbetjening-oppslag"),
            Pair("nav.auth.client_secret", "mySecret"),

            Pair("nav.auth.issuers.0.alias", "login-service-v1"),
            Pair("nav.auth.issuers.0.discovery_endpoint", wireMockServer!!.getLoginServiceV1WellKnownUrl())
        )
        return map.toMap()
    }

    private fun String.getAsJson() = JSONObject(this.httpGet().responseString().third.component1())
}
