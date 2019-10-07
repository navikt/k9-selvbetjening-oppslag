package no.nav.k9.utgaende.rest

import org.json.JSONObject
import org.slf4j.Logger

internal fun Logger.restKall(url: String) = info("Utgående kall til $url")
internal fun Logger.logResponse(response: Any) = debug("Response = '$response'")
internal fun JSONObject.getJsonObjectOrNull(key: String) = if (has(key) && !isNull(key)) getJSONObject(key) else null
internal fun JSONObject.getStringOrNull(key: String) = if (has(key) && !isNull(key) && !getString(key).isBlank()) getString(key) else null