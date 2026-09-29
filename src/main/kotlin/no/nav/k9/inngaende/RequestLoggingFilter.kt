package no.nav.k9.inngaende

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

private const val MDC_CORRELATION_ID = "correlation_id"
private const val MDC_REQUEST_ID = "request_id"
private const val X_REQUEST_ID = "X-Request-ID"
private val ekskluderteStier = setOf("/metrics", "/isready", "/isalive", "/health", "/internal/pre-stop", "/favicon.ico")

internal class RequestLoggingFilter : OncePerRequestFilter() {
    private val logger = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val correlationId = request.getHeader(CorrelationIdVerifier.HEADER)?.trim()?.takeIf { CorrelationIdVerifier.erGyldig(it) }
        val requestId = request.getHeader(X_REQUEST_ID)?.trim()?.takeIf { CorrelationIdVerifier.erGyldig(it) }
            ?: UUID.randomUUID().toString()

        correlationId?.let { MDC.put(MDC_CORRELATION_ID, it) }
        MDC.put(MDC_REQUEST_ID, requestId)

        val logges = request.requestURI !in ekskluderteStier
        try {
            if (logges) logger.info("Request ${request.method} ${request.requestURI}${request.queryString?.let { "?$it" } ?: ""}")
            filterChain.doFilter(request, response)
        } finally {
            if (logges) logger.info("${response.status}: ${request.method} - ${request.requestURI}")
            MDC.remove(MDC_CORRELATION_ID)
            MDC.remove(MDC_REQUEST_ID)
            MDC.remove(MDC_ID_TOKEN_JTI)
        }
    }
}
