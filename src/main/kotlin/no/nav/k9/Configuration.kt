package no.nav.k9

import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import no.nav.helse.dusseldorf.ktor.core.getRequiredString
import java.net.URI

@KtorExperimentalAPI
internal fun ApplicationConfig.personV3Url() = URI(getRequiredString("nav.register_urls.person_v3", secret = false))

@KtorExperimentalAPI
internal fun ApplicationConfig.aktørV1Url() = URI(getRequiredString("nav.register_urls.aktør_v1", secret = false))

@KtorExperimentalAPI
internal fun ApplicationConfig.wsStsUrl() = URI(getRequiredString("nav.auth.ws.sts_url", secret = false))

@KtorExperimentalAPI
internal fun ApplicationConfig.wsUsername() = getRequiredString("nav.auth.ws.username", secret = false)

@KtorExperimentalAPI
internal fun ApplicationConfig.wsPassword() = getRequiredString("nav.auth.ws.password", secret = true)