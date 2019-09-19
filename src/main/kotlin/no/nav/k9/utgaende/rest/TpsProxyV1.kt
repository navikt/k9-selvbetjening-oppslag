package no.nav.k9.utgaende.rest

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.k9.inngaende.correlationId
import no.nav.k9.inngaende.idToken
import no.nav.k9.inngaende.oppslag.Ident
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import kotlin.coroutines.coroutineContext

internal class TpsProxyV1 (
    baseUrl: URI
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(TpsProxyV1::class.java)
    }

    private val personUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("innsyn", "person")
    ).toString()

    private val barnUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("innsyn", "barn")
    ).toString()

    internal suspend fun person(ident: Ident) : TpsPerson {
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

        val json = Retry.retry(
            operation = "hente-person",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val (request,_, result) = Operation.monitored(
                app = "k9-selvbetjening-oppslag",
                operation = "hente-person",
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }

            result.fold(
                { success -> JSONObject(success) },
                { error ->
                    logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved henting av person.")
                }
            )
        }

        val navn = json.getJSONObject("navn")

        return TpsPerson(
            fornavn = navn.getString(navn.getString("fornavn")),
            mellomnavn = if (navn.has("mellomnavn")) navn.getString("mellomnavn") else null,
            etternavn = navn.getString("slektsnavn"),
            fødselsdato = LocalDate.parse(json.getString("foedselsdato"))
        )
    }

    internal suspend fun barn(ident: Ident) : Set<TpsBarn> {
        val authorizationHeader = "Bearer ${coroutineContext.idToken().value}"

        val httpRequest = barnUrl
            .httpGet()
            .header(
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.Accept to "application/json",
                NavHeaders.ConsumerId to NavHeaderValues.ConsumerId,
                NavHeaders.PersonIdent to ident.value,
                NavHeaders.CallId to coroutineContext.correlationId().value
            )

        logger.restKall(barnUrl)

        val json = Retry.retry(
            operation = "hente-barn",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val (request,_, result) = Operation.monitored(
                app = "k9-selvbetjening-oppslag",
                operation = "hente-barn",
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }

            result.fold(
                { success -> JSONArray(success) },
                { error ->
                    logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved henting av person.")
                }
            )
        }

        if (json.isEmpty) return emptySet()

        return json
            .asSequence()
            .map { it as JSONObject }
            .map {
                val sammensattNavn = it.getString("forkortetNavn")
                val splittetNavn = sammensattNavn.splittetNavn()
                val dødsdato = it.getJsonObjectOrNull("doedsdato")?.getStringOrNull("dato")

                TpsBarn(
                    fornavn = splittetNavn.first,
                    mellomnavn = splittetNavn.second,
                    etternavn = splittetNavn.third,
                    fødselsdato = LocalDate.parse(it.getString("foedselsdato")),
                    dødsdato = if (dødsdato != null) LocalDate.parse(dødsdato) else null
                )
            }
            .toSet()
    }
}

private fun String.splittetNavn() : Triple<String, String?, String> {
    val splittet = this.split(" ")
    val fornavn = splittet.first()
    val etternavn = splittet.last()
    val mellomnavn = this
        .removePrefix(fornavn)
        .removeSuffix(etternavn)
        .trim()
    return Triple(fornavn, if (mellomnavn.isBlank()) null else mellomnavn, etternavn)
}

internal data class TpsPerson(
    internal val fornavn: String,
    internal val mellomnavn: String?,
    internal val etternavn: String,
    internal val fødselsdato: LocalDate
)
internal data class TpsBarn(
    internal val fornavn: String,
    internal val mellomnavn: String?,
    internal val etternavn: String,
    internal val fødselsdato: LocalDate,
    internal val dødsdato: LocalDate?
)