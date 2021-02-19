package no.nav.k9.utgaende.rest

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.inngaende.correlationId
import no.nav.k9.inngaende.idToken
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.inngaende.oppslag.iDag
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import kotlin.coroutines.coroutineContext

internal class TpsProxyV1(
    baseUrl: URI
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(TpsProxyV1::class.java)
    }

    private val navnUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("navn")
    ).toString()

    private val personUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("innsyn", "person")
    ).toString()

    internal suspend fun person(ident: Ident): TpsPerson {
        val authorizationHeader = "Bearer ${coroutineContext.idToken().value}"

        val httpRequest = personUrl
            .httpGet()
            .header(
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.Accept to "application/json",
                NavHeaders.ConsumerId to NavHeaderValues.ConsumerId,
                NavHeaders.PersonIdent to ident.value,
                NavHeaders.CallId to coroutineContext.correlationId().value
            )

        logger.restKall(personUrl)

        val json: JSONObject = Retry.retry(
            operation = "hente-person",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val (request, _, result) = Operation.monitored(
                app = NavHeaderValues.ConsumerId,
                operation = "hente-person",
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }

            result.fold(
                { success -> JSONObject(success) },
                { error ->
                    logger.error(
                        "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                    )
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved henting av person.")
                }
            )
        }

        logger.logResponse(json)

        return TpsPerson(
            kontonummer = json.kontonummer()
        )
    }
}


internal data class TpsPerson(
    internal val kontonummer: String?
)

internal data class ForkortetNavn(private val value: String) {
    private companion object {
        private const val MaksLengdePåForkortetNavnFraTpsProxy = 25
    }

    internal val fornavn: String
    internal val etternavn: String
    internal val erKomplett: Boolean

    init {
        val splittetNavn = value
            .split(" ")
            .filterNot { it.isBlank() }
        fornavn = splittetNavn.fornavn()
        etternavn = splittetNavn.etternavn()
        erKomplett =
            value.length < MaksLengdePåForkortetNavnFraTpsProxy && fornavn.isNotBlank() && etternavn.isNotBlank()
    }
}

private fun List<String>.etternavn() = if (isEmpty()) "" else first()
private fun List<String>.fornavn() = if (size > 1) subList(1, size).joinToString(" ") else ""

private fun LocalDate.erFørEllerLik(dato: LocalDate) = isBefore(dato) || isEqual(dato)

private fun JSONObject.kontonummer() : String? {
    val kontonummer = getJsonObjectOrNull("kontonummer") ?: return null
    val nummer = kontonummer.getStringOrNull("nummer") ?: return null
    val fraOgMed = kontonummer.getStringOrNull("datoFraOgMed")
    return when {
        fraOgMed == null -> nummer
        LocalDate.parse(fraOgMed).erFørEllerLik(iDag()) -> nummer
        else -> null
    }
}
