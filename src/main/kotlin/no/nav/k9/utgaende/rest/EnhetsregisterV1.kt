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
import kotlin.coroutines.coroutineContext

internal class EnhetsregisterV1(
    private val baseUrl: URI
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(EnhetsregisterV1::class.java)
    }

    private fun nøkkelInfoUrl(organisasjonsnummer: EnhetOrganisasjonsnummer) = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("organisasjon", organisasjonsnummer.value, "noekkelinfo")
    ).toString()


    internal suspend fun nøkkelinfo(organisasjonsnummer: EnhetOrganisasjonsnummer) : EnhetOrganisasjon {
        val httpRequest = nøkkelInfoUrl(organisasjonsnummer)
            .httpGet()
            .header(
                HttpHeaders.Accept to "application/json",
                "Nav-Consumer-Id" to "k9-selvbetjening-oppslag",
                "Nav-Call-Id" to coroutineContext.correlationId().value
            )

        val json = Retry.retry(
            operation = "hente-organisasjon-noekkelinfo",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val (request,_, result) = Operation.monitored(
                app = "k9-selvbetjening-oppslag",
                operation = "hente-organisasjon-noekkelinfo",
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }

            result.fold(
                { success -> JSONObject(success) },
                { error ->
                    logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved henting av Nøkkelinfo for organisasjon ${organisasjonsnummer.value}")
                }
            )
        }

        if (!json.has("navn")) {
            logger.warn("Inget navn tilgjenelig for organisasjon ${organisasjonsnummer.value}. Response = '$json'")
            return EnhetOrganisasjon(
                organisasjonsnummer = organisasjonsnummer,
                navn = null
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

        return EnhetOrganisasjon(
            organisasjonsnummer = organisasjonsnummer,
            navn = sammensattNavn
        )
    }

    private fun JSONObject.navnlinje(nummer: Int) : String? {
        val key = "navnlinje$nummer"
        return if (has(key)) getString(key) else null
    }
}

internal data class EnhetOrganisasjon(internal val organisasjonsnummer: EnhetOrganisasjonsnummer, internal val navn: String?)
internal data class EnhetOrganisasjonsnummer(internal val value: String)