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

    fun borgerToken(): String = oAuth2AccessTokenService.token(tokenxAaregProperties, TOKENX_AAREG)

    private companion object {
        private const val TOKENX_AAREG = "tokenx-aareg"
    }
}
