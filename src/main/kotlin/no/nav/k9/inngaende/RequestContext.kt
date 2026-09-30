package no.nav.k9.inngaende

import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * Correlation-id for requesten som behandles på denne tråden. Verdien er validert av CallIdInterceptor.
 * Fungerer i suspending functions fordi controllerne kaller domenet med runBlocking på request-tråden.
 */
internal fun currentCorrelationId(): CorrelationId {
    val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        ?: throw IllegalStateException("Ingen aktiv request.")
    val header = request.getHeader(CorrelationIdVerifier.HEADER)
        ?: throw IllegalStateException("Request mangler ${CorrelationIdVerifier.HEADER}.")
    return CorrelationId(header.trim())
}
