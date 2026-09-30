package no.nav.k9.inngaende

import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * Correlation-id for requesten som behandles på denne tråden. Verdien er validert av CallIdInterceptor.
 * Leser fra tråden requesten kjører på, som også token-client-spring gjør for token exchange.
 */
internal fun currentCorrelationId(): CorrelationId {
    val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        ?: throw IllegalStateException("Ingen aktiv request.")
    val header = request.getHeader(CorrelationIdVerifier.HEADER)
        ?: throw IllegalStateException("Request mangler ${CorrelationIdVerifier.HEADER}.")
    return CorrelationId(header.trim())
}
