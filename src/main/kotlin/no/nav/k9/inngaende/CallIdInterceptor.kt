package no.nav.k9.inngaende

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.k9.http.ParameterType
import no.nav.k9.http.ValidationErrorResponseException
import no.nav.k9.http.Violation
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Krever gyldig X-Correlation-ID. Kjører før interceptoren til token-support, slik at
 * manglende correlation-id gir 400 også når token mangler (samme rekkefølge som i Ktor-versjonen).
 */
internal class CallIdInterceptor : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val correlationId = request.getHeader(CorrelationIdVerifier.HEADER)?.trim()
        if (correlationId == null || !CorrelationIdVerifier.erGyldig(correlationId)) {
            throw ValidationErrorResponseException(
                setOf(
                    Violation(
                        parameterName = CorrelationIdVerifier.HEADER,
                        parameterType = ParameterType.HEADER,
                        reason = "Correlation ID må settes.",
                        invalidValue = null
                    )
                )
            )
        }
        return true
    }
}
