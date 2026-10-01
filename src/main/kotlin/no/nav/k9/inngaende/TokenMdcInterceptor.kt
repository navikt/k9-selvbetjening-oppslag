package no.nav.k9.inngaende

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.MDC
import org.springframework.web.servlet.HandlerInterceptor

internal const val MDC_ID_TOKEN_JTI = "id_token_jti"

// Kjører etter at token-support har validert tokenet. Fjernes av RequestLoggingFilter.
internal class TokenMdcInterceptor(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
) : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        runCatching {
            tokenValidationContextHolder.getTokenValidationContext().firstValidToken
                ?.jwtTokenClaims?.getStringClaim("jti")
        }.getOrNull()?.let { MDC.put(MDC_ID_TOKEN_JTI, it) }
        return true
    }
}
