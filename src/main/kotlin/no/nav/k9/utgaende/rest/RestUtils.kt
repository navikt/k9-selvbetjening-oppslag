package no.nav.k9.utgaende.rest

import net.minidev.json.parser.JSONParser
import no.nav.helse.dusseldorf.ktor.core.templateQueryParameters
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger

private object RestUtils {
    internal val parser = JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE)
}

internal fun Logger.restKall(url: String, urlTemplate: Boolean = false) = info("Utgående kall til ${if (urlTemplate) url.templateQueryParameters() else url}")
internal fun Logger.logResponse(response: Any) = debug("Response = '$response'")
internal fun JSONObject.getJsonObjectOrNull(key: String) = if (has(key) && !isNull(key)) getJSONObject(key) else null
internal fun JSONObject.getJsonArrayOrEmpty(key: String) = if (has(key) && !isNull(key)) getJSONArray(key) else JSONArray()

internal fun JSONObject.getStringOrNull(key: String) = if (has(key) && !isNull(key) && !getString(key).isBlank()) getString(key) else null
/**
 * ArbeidsgiverOgArbeidstakerRegisterV1 returnerer ugyldig json som gir org.json.JSONException: Duplicate key
 * org.json.simple håndterer dette med å ta sisste key istedenfor å feile. Wrapper håndteringen for å unngå denne feilen.
 */
internal fun String.somJsonArray() = JSONArray((RestUtils.parser.parse(this) as net.minidev.json.JSONArray).toJSONString())
