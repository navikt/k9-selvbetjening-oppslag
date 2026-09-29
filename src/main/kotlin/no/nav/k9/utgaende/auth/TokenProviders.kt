package no.nav.k9.utgaende.auth

import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.stereotype.Service

private fun ClientConfigurationProperties.registrering(navn: String): ClientProperties =
    registration[navn] ?: throw IllegalStateException("Fant ikke oauth2-klientkonfigurasjon for $navn")

private fun OAuth2AccessTokenService.token(properties: ClientProperties, navn: String): String =
    getAccessToken(properties).access_token ?: throw IllegalStateException("Kunne ikke hente access token for $navn")

/**
 * Tokens mot PDL. Registreringsnavnene følger sif-tilgangskontroll.
 * Borgertoken veksles fra innkommende token (tokenx), så metoden må kalles på request-tråden.
 */
@Service
class PdlAuthService(
    oauth2Config: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
) {
    private val tokenxPdlProperties = oauth2Config.registrering(TOKENX_PDL_API)
    private val azurePdlProperties = oauth2Config.registrering(AZURE_PDL_API)

    fun borgerToken(): String = oAuth2AccessTokenService.token(tokenxPdlProperties, TOKENX_PDL_API)
    fun systemToken(): String = oAuth2AccessTokenService.token(azurePdlProperties, AZURE_PDL_API)

    private companion object {
        private const val TOKENX_PDL_API = "tokenx-pdl-api"
        private const val AZURE_PDL_API = "azure-pdl-api"
    }
}

@Service
class AaregAuthService(
    oauth2Config: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
) {
    private val tokenxAaregProperties = oauth2Config.registrering(TOKENX_AAREG)

    fun borgerToken(): String = oAuth2AccessTokenService.token(tokenxAaregProperties, TOKENX_AAREG)

    private companion object {
        private const val TOKENX_AAREG = "tokenx-aareg"
    }
}
