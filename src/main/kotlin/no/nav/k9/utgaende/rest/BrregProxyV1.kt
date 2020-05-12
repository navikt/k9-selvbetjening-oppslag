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
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.inngaende.oppslag.filtrertForetak
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import kotlin.coroutines.coroutineContext

internal class BrregProxyV1(
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    private val hentePersonRolleoversiktScopes: Set<String> = setOf("openid")
) {
    private val hentePersonRolleoversiktUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("person", "rolleoversikt")
    ).toString()

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    internal suspend fun foretak(
        ident: Ident
    ) : Set<Foretak> {
        val navConsumerIdHeader = cachedAccessTokenClient.getAccessToken(hentePersonRolleoversiktScopes).asAuthoriationHeader()
        //val authorizationHeader = "Bearer ${coroutineContext.idToken().value}" TODO: https://github.com/navikt/k9-selvbetjening-oppslag/issues/18
        val authorizationHeader = navConsumerIdHeader

        val httpRequest = hentePersonRolleoversiktUrl
            .httpGet()
            .header(
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.Accept to "application/json",
                NavHeaders.ConsumerId to NavHeaderValues.ConsumerId,
                NavHeaders.CallId to coroutineContext.correlationId().value,
                NavHeaders.ConsumerToken to navConsumerIdHeader,
                NavHeaders.PersonIdent to ident.value
            )

        logger.restKall(hentePersonRolleoversiktUrl)

        val json = Retry.retry(
            operation = OperationHentePersonRolleoversikt,
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val (request,_, result) = Operation.monitored(
                app = NavHeaderValues.ConsumerId,
                operation = OperationHentePersonRolleoversikt,
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }

            result.fold(
                { success -> JSONObject(success) },
                { error ->
                    logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved henting av roller for person.")
                }
            )
        }

        logger.logResponse(json)

        json.forsikreIkkeTekniskFeil()

        val roller = json.getJsonArrayOrEmpty("rolle")

        if (roller.isEmpty) {
            logger.info("Har ikke noen roller i noen foretak.")
            return setOf()
        }

        val foretak = mutableSetOf<Foretak>()

        roller.map { it as JSONObject }.forEach {
            foretak.leggTil(
                Foretak(
                    organisasjonsnummer = it.getString("orgnr"),
                    registreringsdato = LocalDate.parse(it.getString("registreringsDato")),
                    rollebeskrivelser = setOf(it.getString("rollebeskrivelse"))
                )
            )
        }

        logger.info("Har roller i ${foretak.size} foretak.")
        val filtrert = foretak.filtrerPåStatuskoder(json)
        logger.filtrertForetak("Statuskoder", filtrert.size)
        return filtrert
    }

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(BrregProxyV1::class.java)
        private const val OperationHentePersonRolleoversikt = "hente-person-rolleoversikt"
    }
}

private fun MutableSet<Foretak>.leggTil(foretak: Foretak) {
    val eksisterendeForetak = firstOrNull { it.organisasjonsnummer == foretak.organisasjonsnummer }

    if (eksisterendeForetak != null) {
        val oppdatertForetak = Foretak(
            organisasjonsnummer = eksisterendeForetak.organisasjonsnummer,
            registreringsdato = when (foretak.registreringsdato.isBefore(eksisterendeForetak.registreringsdato)) {
                true -> foretak.registreringsdato
                false -> eksisterendeForetak.registreringsdato
            },
            rollebeskrivelser = eksisterendeForetak.rollebeskrivelser.plus(foretak.rollebeskrivelser)
        )
        remove(eksisterendeForetak)
        add(oppdatertForetak)
    } else {
        add(foretak)
    }
}

internal data class Foretak(
    val organisasjonsnummer: String,
    val registreringsdato: LocalDate,
    val rollebeskrivelser: Set<String>
)