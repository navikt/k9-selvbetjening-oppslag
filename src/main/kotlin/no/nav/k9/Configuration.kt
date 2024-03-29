package no.nav.k9

import io.ktor.server.config.ApplicationConfig
import no.nav.helse.dusseldorf.ktor.core.getRequiredString
import java.net.URI

internal fun ApplicationConfig.pdlUrl() = URI(getRequiredString("nav.register_urls.pdl_url", secret = false))
internal fun ApplicationConfig.pdlApiTokenxAudience() = getRequiredString("nav.auth.pdl_api_tokenx_audience", secret = false)
internal fun ApplicationConfig.pdlApiAzureAudience() = getRequiredString("nav.auth.pdl_api_azure_audience", secret = false)

internal fun ApplicationConfig.aaregTokenxAudience() = getRequiredString("nav.auth.aareg_tokenx_audience", secret = false)
internal fun ApplicationConfig.enhetsregisterV1Url() = URI(getRequiredString("nav.register_urls.enhetsregister_v1", secret = false))
internal fun ApplicationConfig.arbeidsgiverOgArbeidstakerV2Url() = URI(getRequiredString("nav.register_urls.arbeidsgiver_og_arbeidstaker_v2", secret = false))
