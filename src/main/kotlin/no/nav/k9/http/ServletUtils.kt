package no.nav.k9.http

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.context.request.ServletWebRequest
import java.net.URI

internal fun ServletWebRequest.respondProblemDetails(
    status: HttpStatus,
    title: String,
    type: URI,
    detail: String,
    properties: Map<String, Any> = mapOf(),
): ProblemDetail {
    val problemDetail = ProblemDetail.forStatusAndDetail(status, detail)
    problemDetail.title = title
    problemDetail.type = type
    problemDetail.instance = runCatching { URI(request.requestURI) }.getOrNull()
    properties.forEach { (key, value) -> problemDetail.setProperty(key, value) }
    return problemDetail
}
