package no.nav.k9.integrasjon.aareg

import no.nav.k9.integrasjon.common.registrering
import no.nav.k9.integrasjon.common.token
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.springframework.stereotype.Service

@Service
class AaregAuthService(
    oauth2Config: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
) {
    private val tokenxAaregProperties = oauth2Config.registrering(TOKENX_AAREG)

    private val azureAaregProperties = oauth2Config.registrering(AZURE_AAREG)

    fun borgerToken(): String = oAuth2AccessTokenService.token(tokenxAaregProperties, TOKENX_AAREG)

    // Kopi av PdlAuthService.systemToken(). Token for appen selv (client_credentials), ikke for en innlogget person.
    // Brukes bare fra /system-endepunktene, som krever Azure-token med rollen access_as_application.
    fun systemToken(): String = oAuth2AccessTokenService.token(azureAaregProperties, AZURE_AAREG)

    private companion object {
        private const val TOKENX_AAREG = "tokenx-aareg"
        private const val AZURE_AAREG = "azure-aareg"
    }
}
