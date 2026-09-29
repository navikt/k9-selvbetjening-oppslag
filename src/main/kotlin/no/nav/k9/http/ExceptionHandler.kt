package no.nav.k9.http

import no.nav.k9.integrasjon.pdl.TilgangNektetException
import no.nav.security.token.support.core.exceptions.JwtTokenMissingException
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.URI

@ControllerAdvice
internal class ExceptionHandler : ResponseEntityExceptionHandler() {

    private companion object {
        private val log: Logger = LoggerFactory.getLogger(ExceptionHandler::class.java)
    }

    // detail er bevisst en fast tekst: meldinger fra oppstrømstjenester kan inneholde response body.
    @ExceptionHandler(value = [Exception::class])
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun håndtereGeneriskException(exception: Exception, request: ServletWebRequest): ProblemDetail {
        log.error("Uventet feil ved ${request.request.method} ${request.request.requestURI}", exception)
        return request.respondProblemDetails(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            title = "Et uventet feil har oppstått",
            type = URI("/problem-details/internal-server-error"),
            detail = "Det oppstod en uventet feil ved behandling av forespørselen."
        )
    }

    @ExceptionHandler(TilgangNektetException::class)
    @ResponseStatus(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS)
    fun håndtereTilgangNektetException(exception: TilgangNektetException, request: ServletWebRequest): ProblemDetail {
        val problemDetails = request.respondProblemDetails(
            status = HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS,
            title = "tilgangskontroll-feil",
            type = URI("/problem-details/tilgangskontroll-feil"),
            detail = "Policy decision: ${exception.policyException.decision} - Reason: ${exception.policyException.reason}"
        )
        log.info("{}", problemDetails)
        return problemDetails
    }

    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun håndtereTokenUnauthorizedException(
        exception: JwtTokenUnauthorizedException,
        request: ServletWebRequest,
    ): ProblemDetail {
        val problemDetails = request.respondProblemDetails(
            status = HttpStatus.UNAUTHORIZED,
            title = "Ikke autentisert",
            type = URI("/problem-details/uautentisert-forespørsel"),
            detail = exception.message ?: ""
        )
        log.debug("{}", problemDetails)
        return problemDetails
    }

    @ExceptionHandler(JwtTokenValidatorException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun håndtereTokenValidatorException(
        exception: JwtTokenValidatorException,
        request: ServletWebRequest,
    ): ProblemDetail {
        val problemDetails = request.respondProblemDetails(
            status = HttpStatus.FORBIDDEN,
            title = "Ikke uautorisert",
            type = URI("/problem-details/uautorisert-forespørsel"),
            detail = exception.message ?: ""
        )
        log.debug("{}", problemDetails)
        return problemDetails
    }

    @ExceptionHandler(JwtTokenMissingException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun håndtereJwtTokenMissingException(
        exception: JwtTokenMissingException,
        request: ServletWebRequest,
    ): ProblemDetail {
        val problemDetails = request.respondProblemDetails(
            status = HttpStatus.UNAUTHORIZED,
            title = "Ingen token funnet.",
            type = URI("/problem-details/mangler-token"),
            detail = exception.message ?: ""
        )
        log.debug("{}", problemDetails)
        return problemDetails
    }
}
