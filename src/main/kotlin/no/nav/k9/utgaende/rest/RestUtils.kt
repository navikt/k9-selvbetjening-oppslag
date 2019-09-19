package no.nav.k9.utgaende.rest

import org.json.JSONObject
import org.slf4j.Logger

internal fun Logger.restKall(url: String) = info("Utgående kall til $url")

internal fun JSONObject.getJsonObjectOrNull(key: String) = if (has(key)) getJSONObject(key) else null
internal fun JSONObject.getJsonArrayOrNull(key: String) = if (has(key)) getJSONArray(key) else null
internal fun JSONObject.getStringOrNull(key: String) = if (has(key)) getString(key) else null