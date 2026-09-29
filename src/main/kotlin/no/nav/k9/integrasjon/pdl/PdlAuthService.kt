package no.nav.k9.integrasjon.pdl

import no.nav.k9.integrasjon.common.registrering
import no.nav.k9.integrasjon.common.token
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.stereotype.Service

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
