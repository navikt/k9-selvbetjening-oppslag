package no.nav.k9.utgaende.rest

import kotlinx.coroutines.currentCoroutineContext
import no.nav.k9.inngaende.correlationId
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.retry.support.RetryTemplate
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.LocalDate

internal class EnhetsregisterV1(
    private val baseUrl: URI,
    restClientBuilder: RestClient.Builder,
    private val retryTemplate: RetryTemplate,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(EnhetsregisterV1::class.java)
        private const val NØKKELINFO_PATH = "/organisasjon/{organisasjonsnummer}/noekkelinfo"
    }

    private val restClient = restClientBuilder.baseUrl(baseUrl.toString().trimEnd('/')).build()

    internal suspend fun nøkkelinfo(organisasjonsnummer: String) : Enhet {
        val callId = currentCoroutineContext().correlationId().value

        logger.restKall("${baseUrl.toString().trimEnd('/')}$NØKKELINFO_PATH")

        val json = retryTemplate.execute<JSONObject, RuntimeException> {
            val response = restClient.get()
                .uri(NØKKELINFO_PATH, organisasjonsnummer)
                .accept(MediaType.APPLICATION_JSON)
                .header(NavHeaders.ConsumerId, NavHeaderValues.ConsumerId)
                .header(NavHeaders.CallId, callId)
                .retrieve()
                .body(String::class.java)
                ?: throw IllegalStateException("Tom respons ved henting av Nøkkelinfo for organisasjon $organisasjonsnummer")
            JSONObject(response)
        }

        logger.logResponse(json)

        if (!json.has("navn")) {
            logger.warn("Ingen navn tilgjenelig for organisasjon ${organisasjonsnummer}. Response = '$json'")
            return Enhet(
                organisasjonsnummer = organisasjonsnummer,
                navn = null,
                enhetstype = json.enhetstype(),
                opphørsdato = json.opphørsdato()
            )
        }
        val navn = json.getJSONObject("navn")

        val navnlinjer = listOf(
            navn.navnlinje(1),
            navn.navnlinje(2),
            navn.navnlinje(3),
            navn.navnlinje(4),
            navn.navnlinje(5))
            .filterNot { it.isNullOrBlank() }

        val sammensattNavn = if (navnlinjer.isEmpty()) null else {
            navnlinjer.joinToString(", ")
        }

        return Enhet(
            organisasjonsnummer = organisasjonsnummer,
            navn = sammensattNavn,
            enhetstype = json.enhetstype(),
            opphørsdato = json.opphørsdato()
        )
    }

    private fun JSONObject.navnlinje(nummer: Int) = getStringOrNull("navnelinje$nummer")
    private fun JSONObject.enhetstype() = getStringOrNull("enhetstype")
    private fun JSONObject.opphørsdato() : LocalDate? {
        val stringValue = getStringOrNull("opphoersdato") ?: return null
        return LocalDate.parse(stringValue)
    }
}

internal data class Enhet(
    internal val organisasjonsnummer: String,
    internal val navn: String?,
    internal val enhetstype: String?,
    internal val opphørsdato: LocalDate?
)

