package no.nav.k9.integrasjon.common

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.Logger

internal fun String.templateQueryParameters(): String {
    val urlParts = split("?")
    if (urlParts.size < 2) return this

    val query = urlParts[1].split("&").joinToString("&") {
        it.replaceAfter("=", "{${it.substringBefore("=")}}")
    }

    return urlParts[0] + "?" + query
}

internal fun Logger.restKall(url: String, urlTemplate: Boolean = false) = info("Utgående kall til ${if (urlTemplate) url.templateQueryParameters() else url}")
internal fun Logger.logResponse(response: Any) = debug("Response = '$response'")
internal fun JsonNode.getStringOrNull(key: String): String? =
    get(key)?.takeUnless { it.isNull }?.asText()?.takeUnless { it.isBlank() }

internal fun JsonNode.påkrevd(key: String): JsonNode =
    get(key)?.takeUnless { it.isNull } ?: throw IllegalStateException("Mangler '$key' i respons.")
