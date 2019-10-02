package no.nav.k9

import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.core.getRequiredString
import java.net.URI

@KtorExperimentalAPI
internal fun ApplicationConfig.tpsProxyV1Url() = URI(getRequiredString("nav.register_urls.tps_proxy_v1", secret = false))

@KtorExperimentalAPI
internal fun ApplicationConfig.personV3Url() = URI(getRequiredString("nav.register_urls.person_v3", secret = false))

@KtorExperimentalAPI
internal fun ApplicationConfig.aktørV1Url() = URI(getRequiredString("nav.register_urls.aktoer_v1", secret = false))

@KtorExperimentalAPI
internal fun ApplicationConfig.arbeidsgiverOgArbeidstakerV1Url() = URI(getRequiredString("nav.register_urls.arbeidsgiver_og_arbeidstaker_v1", secret = false))

@KtorExperimentalAPI
internal fun ApplicationConfig.enhetsregisterV1Url() = URI(getRequiredString("nav.register_urls.enhetsregister_v1", secret = false))

@KtorExperimentalAPI
internal fun ApplicationConfig.restTokenUrl() = URI(getRequiredString("nav.auth.rest_token_url", secret = false))

@KtorExperimentalAPI
internal fun ApplicationConfig.clientId() = getRequiredString("nav.auth.client_id", secret = false)

@KtorExperimentalAPI
internal fun ApplicationConfig.clientSecret() = getRequiredString("nav.auth.client_secret", secret = true)