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
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import kotlin.coroutines.coroutineContext

internal class ArbeidsgiverOgArbeidstakerRegisterV1 (
    baseUrl: URI,
    accessTokenClient: AccessTokenClient,
    private val henteArbeidsforholdPerArbeidstakerScopes: Set<String> = setOf("openid")
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(ArbeidsgiverOgArbeidstakerRegisterV1::class.java)
    }

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    private val arbeidsforholdPerArbeidstakerUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("arbeidstaker", "arbeidsforhold")
    )

    private fun urlMedFraOgMedTilOgMed(url: URI, fraOgMed: LocalDate, tilOgMed: LocalDate) = Url.buildURL(
        baseUrl = url,
        queryParameters = mapOf(
            "ansettelsesperiodeFom" to listOf(fraOgMed.toString()),
            "ansettelsesperiodeTom" to listOf(tilOgMed.toString())
        )
    ).toString()

    internal suspend fun arbeidsforhold(
        ident: Ident,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate
    ) : Arbeidsforhold {
        val navConsumerIdHeader = cachedAccessTokenClient.getAccessToken(henteArbeidsforholdPerArbeidstakerScopes).asAuthoriationHeader()
        val authorizationHeader = "Bearer ${coroutineContext.idToken().value}"
        val url = urlMedFraOgMedTilOgMed(arbeidsforholdPerArbeidstakerUrl, fraOgMed, tilOgMed)

        val httpRequest = url
            .httpGet()
            .header(
                HttpHeaders.Authorization to authorizationHeader,
                HttpHeaders.Accept to "application/json",
                NavHeaders.ConsumerToken to navConsumerIdHeader,
                NavHeaders.ConsumerId to NavHeaderValues.ConsumerId,
                NavHeaders.CallId to coroutineContext.correlationId().value,
                NavHeaders.PersonIdent to ident.value
            )

        logger.restKall(url, true)

        val json = Retry.retry(
            operation = "hente-arbeidsforhold-per-arbeidstaker",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val (request,_, result) = Operation.monitored(
                app = NavHeaderValues.ConsumerId,
                operation = "hente-arbeidsforhold-per-arbeidstaker",
                resultResolver = { 200 == it.second.statusCode }
            ) { httpRequest.awaitStringResponseResult() }

            result.fold(
                { success -> success.somJsonArray() },
                { error ->
                    logger.error("Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'")
                    logger.error(error.toString())
                    throw IllegalStateException("Feil ved henting av arbeidsforhold per arbeidstaker")
                }
            )
        }

        logger.logResponse(json)


        if (json.isEmpty) return Arbeidsforhold(
            organisasjoner = emptySet()
        )

        val organisasjoner = json
            .asSequence()
            .map { it as JSONObject }
            .filter { it.has("arbeidsgiver") }
            .filter { it.getJSONObject("arbeidsgiver").has("organisasjonsnummer") }
            .map { OrganisasjonArbeidsforhold(
                organisasjonsnummer = it.getJSONObject("arbeidsgiver").getString("organisasjonsnummer")
            )}
            .toSet()

        return Arbeidsforhold(
            organisasjoner = organisasjoner
        )
    }
}

internal data class OrganisasjonArbeidsforhold(
    internal val organisasjonsnummer: String
)
internal data class Arbeidsforhold(
    internal val organisasjoner: Set<OrganisasjonArbeidsforhold>
)

