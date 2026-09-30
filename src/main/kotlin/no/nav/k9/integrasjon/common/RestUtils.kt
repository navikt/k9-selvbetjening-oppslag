package no.nav.k9.integrasjon.common

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.Logger

internal fun Logger.restKall(url: String) = info("Utgående kall til $url")
internal fun Logger.logResponse(response: Any) = debug("Response = '$response'")
internal fun JsonNode.getStringOrNull(key: String): String? =
    get(key)?.takeUnless { it.isNull }?.asText()?.takeUnless { it.isBlank() }

internal fun JsonNode.påkrevd(key: String): JsonNode =
    get(key)?.takeUnless { it.isNull } ?: throw IllegalStateException("Mangler '$key' i respons.")
