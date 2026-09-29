package no.nav.k9.integrasjon.common

import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties

internal fun ClientConfigurationProperties.registrering(navn: String): ClientProperties =
    registration[navn] ?: throw IllegalStateException("Fant ikke oauth2-klientkonfigurasjon for $navn")

internal fun OAuth2AccessTokenService.token(properties: ClientProperties, navn: String): String =
    getAccessToken(properties).access_token ?: throw IllegalStateException("Kunne ikke hente access token for $navn")
