package no.nav.k9

import io.ktor.config.ApplicationConfig
import no.nav.helse.dusseldorf.ktor.core.getRequiredString
import java.net.URI


internal fun ApplicationConfig.tpsProxyV1Url() = URI(getRequiredString("nav.register_urls.tps_proxy_v1", secret = false))

internal fun ApplicationConfig.pdlUrl() = URI(getRequiredString("nav.register_urls.pdl_url", secret = false))
internal fun ApplicationConfig.pdlApiTokenxAudience() = getRequiredString("nav.auth.pdl_api_tokenx_audience", secret = false)
internal fun ApplicationConfig.pdlApiAzureAudience() = getRequiredString("nav.auth.pdl_api_azure_audience", secret = false)

internal fun ApplicationConfig.arbeidsgiverOgArbeidstakerV1Url() = URI(getRequiredString("nav.register_urls.arbeidsgiver_og_arbeidstaker_v1", secret = false))

internal fun ApplicationConfig.enhetsregisterV1Url() = URI(getRequiredString("nav.register_urls.enhetsregister_v1", secret = false))

internal fun ApplicationConfig.brregProxyV1Url() = URI(getRequiredString("nav.register_urls.brreg_proxy_v1", secret = false))

internal fun ApplicationConfig.restTokenUrl() = URI(getRequiredString("nav.auth.rest_token_url", secret = false))

internal fun ApplicationConfig.clientId() = getRequiredString("nav.auth.client_id", secret = false)

internal fun ApplicationConfig.clientSecret() = getRequiredString("nav.auth.client_secret", secret = true)
