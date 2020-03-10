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
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import kotlin.coroutines.coroutineContext

internal class TpsProxyV1(
    baseUrl: URI,
    private val accessTokenClient: AccessTokenClient,
    private val henteNavnScopes: Set<String> = setOf("openid")
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(TpsProxyV1::class.java)
    }

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    private val navnUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("navn")
    ).toString()

    private val personUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("innsyn", "person")
    ).toString()

    private val barnUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("innsyn", "barn")
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

        val json = Retry.retry(
            operation = "hente-person",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val (request, _, result) = Operation.monitored(
                app = "k9-selvbetjening-oppslag",
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

        val navn = json.getJSONObject("navn")


        return TpsPerson(
            fornavn = navn.getString("fornavn"),
            mellomnavn = navn.getStringOrNull("mellomnavn"),
            etternavn = navn.getString("slektsnavn"),
            fødselsdato = LocalDate.parse(json.getString("foedselsdato"))
        )
    }

    internal suspend fun barn(ident: Ident): Set<TpsBarn> {
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
            val (request, _, result) = Operation.monitored(
                app = "k9-selvbetjening-oppslag",
                operation = "hente-barn",
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }

            result.fold(
                { success -> JSONArray(success) },
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

        if (json.isEmpty) return emptySet()

        return json
            .map { it as JSONObject }
            .map {
                val barnetsIdent = Ident(it.getString("ident"))
                val navn = ForkortetNavn(it.getString("forkortetNavn")).somTpsNavn(barnetsIdent)
                val dødsdato = it.getJsonObjectOrNull("doedsdato")?.getStringOrNull("dato")
                val harSammeAdresse = it.getBoolean("harSammeAdresse")

                TpsBarn(
                    fornavn = navn.fornavn,
                    mellomnavn = navn.mellomnavn,
                    etternavn = navn.etternavn,
                    fødselsdato = LocalDate.parse(it.getString("foedselsdato")),
                    dødsdato = if (dødsdato != null) LocalDate.parse(dødsdato) else null,
                    harSammeAdresse = harSammeAdresse,
                    ident = barnetsIdent
                )
            }
            .toSet()
    }

    private suspend fun navn(ident: Ident): TpsNavn {
        val authorizationHeader = cachedAccessTokenClient
            .getAccessToken(henteNavnScopes)
            .asAuthoriationHeader()

        val httpRequest = navnUrl
            .httpGet()
            .header(
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.Accept to "application/json",
                NavHeaders.ConsumerId to NavHeaderValues.ConsumerId,
                NavHeaders.PersonIdent to ident.value,
                NavHeaders.CallId to coroutineContext.correlationId().value
            )

        logger.restKall(navnUrl)

        val json = Retry.retry(
            operation = "hente-navn",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val (request, _, result) = Operation.monitored(
                app = "k9-selvbetjening-oppslag",
                operation = "hente-navn",
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }

            result.fold(
                { success -> JSONObject(success) },
                { error ->
                    logger.error(
                        "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                    )
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved henting av navn.")
                }
            )
        }

        logger.logResponse(json)

        return TpsNavn(
            fornavn = json.getStringOrNull("fornavn") ?: "",
            mellomnavn = json.getStringOrNull("mellomnavn"),
            etternavn = json.getStringOrNull("etternavn") ?: ""
        )
    }

    private suspend fun ForkortetNavn.somTpsNavn(ident: Ident): TpsNavn {
        return if (erKomplett) {
            TpsNavn(
                fornavn = fornavn,
                mellomnavn = null,
                etternavn = etternavn
            )
        } else navn(ident)
    }
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
    internal val dødsdato: LocalDate?,
    internal val harSammeAdresse: Boolean,
    internal val ident: Ident
)

internal data class TpsNavn(
    internal val fornavn: String,
    internal val mellomnavn: String?,
    internal val etternavn: String
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
