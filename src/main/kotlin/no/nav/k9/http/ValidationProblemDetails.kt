package no.nav.k9.http

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException
import java.net.URI

internal enum class ParameterType {
    QUERY,
    HEADER,
    ENTITY,
}

internal data class Violation(
    val parameterName: String,
    val parameterType: ParameterType,
    val reason: String,
    val invalidValue: String? = null,
)

internal class ValidationErrorResponseException(violations: Set<Violation>) :
    ErrorResponseException(HttpStatus.BAD_REQUEST, validationProblemDetails(violations), null)

private fun validationProblemDetails(violations: Set<Violation>): ProblemDetail =
    ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Requesten inneholder ugyldige parametere.").apply {
        type = URI("/problem-details/invalid-request-parameters")
        title = "invalid-request-parameters"
        setProperty(
            "violations",
            violations.map {
                mapOf(
                    "parameterName" to it.parameterName,
                    "parameterType" to it.parameterType.name,
                    "reason" to it.reason,
                    "invalidValue" to it.invalidValue,
                )
            }
        )
    }
