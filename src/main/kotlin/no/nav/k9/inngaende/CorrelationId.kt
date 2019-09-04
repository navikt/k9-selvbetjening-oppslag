package no.nav.k9.inngaende

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpHeaders

internal data class CorrelationId(internal val value: String)

internal fun ApplicationCall.correlationId() : CorrelationId {
    val correlationId = request.headers[HttpHeaders.XCorrelationId] ?: throw IllegalStateException("CorrelationID ikke satt")
    return CorrelationId(correlationId)
}