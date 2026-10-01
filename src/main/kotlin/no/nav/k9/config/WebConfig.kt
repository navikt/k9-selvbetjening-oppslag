package no.nav.k9.config

import no.nav.k9.inngaende.CallIdInterceptor
import no.nav.k9.inngaende.RequestLoggingFilter
import no.nav.k9.inngaende.TokenMdcInterceptor
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

private val beskyttedeStier = arrayOf("/meg", "/arbeidsgivere", "/system/**")

@Configuration
internal class WebConfig(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
) : WebMvcConfigurer {

    // token-support registrerer sin interceptor med order 0.
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(CallIdInterceptor())
            .addPathPatterns(*beskyttedeStier)
            .order(-1)
        registry.addInterceptor(TokenMdcInterceptor(tokenValidationContextHolder))
            .addPathPatterns(*beskyttedeStier)
            .order(1)
    }

    @Bean("requestLoggingFilter")
    internal fun requestLoggingFilter() = FilterRegistrationBean(RequestLoggingFilter()).apply {
        order = Ordered.HIGHEST_PRECEDENCE + 10
    }
}
