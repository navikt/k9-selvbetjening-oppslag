package no.nav.k9.integrasjon.common

import org.slf4j.Logger
import tools.jackson.databind.JsonNode

internal fun Logger.restKall(url: String) = info("Utgående kall til $url")
internal fun Logger.logResponse(response: Any) = debug("Response = '$response'")
internal fun JsonNode.getStringOrNull(key: String): String? =
    get(key)?.takeUnless { it.isNull }?.asString()?.takeUnless { it.isBlank() }

internal fun JsonNode.påkrevd(key: String): JsonNode =
    get(key)?.takeUnless { it.isNull } ?: throw IllegalStateException("Mangler '$key' i respons.")
