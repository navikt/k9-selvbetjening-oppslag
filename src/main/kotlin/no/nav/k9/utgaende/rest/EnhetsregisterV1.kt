package no.nav.k9.utgaende.rest

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.k9.inngaende.correlationId
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import kotlin.coroutines.coroutineContext

internal class EnhetsregisterV1(
    private val baseUrl: URI
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(EnhetsregisterV1::class.java)
        private const val Operation_HenteOrganisasjonNøkkelinfo = "hente-organisasjon-noekkelinfo"
    }

    private fun nøkkelInfoUrl(organisasjonsnummer: String) = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("organisasjon", organisasjonsnummer, "noekkelinfo")
    ).toString()


    internal suspend fun nøkkelinfo(organisasjonsnummer: String) : Enhet {
        val url = nøkkelInfoUrl(organisasjonsnummer)
        val httpRequest = url
            .httpGet()
            .header(
                HttpHeaders.Accept to "application/json",
                NavHeaders.ConsumerId to NavHeaderValues.ConsumerId,
                NavHeaders.CallId to coroutineContext.correlationId().value
            )

        logger.restKall(url)

        val json = Retry.retry(
            operation = Operation_HenteOrganisasjonNøkkelinfo,
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val (request,_, result) = Operation.monitored(
                app = "k9-selvbetjening-oppslag",
                operation = Operation_HenteOrganisasjonNøkkelinfo,
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }

            result.fold(
                { success -> JSONObject(success) },
                { error ->
                    logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved henting av Nøkkelinfo for organisasjon ${organisasjonsnummer}")
                }
            )
        }

        logger.logResponse(json)

        if (!json.has("navn")) {
            logger.warn("Inget navn tilgjenelig for organisasjon ${organisasjonsnummer}. Response = '$json'")
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

