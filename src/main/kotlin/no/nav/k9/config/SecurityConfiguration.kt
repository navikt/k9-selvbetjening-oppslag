package no.nav.k9.config

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Configuration

@Configuration
@EnableJwtTokenValidation(ignore = ["org.springframework"])
internal class SecurityConfiguration

object Issuers {
    const val TOKEN_X = "tokenx"
    const val AZURE = "azure"
}
