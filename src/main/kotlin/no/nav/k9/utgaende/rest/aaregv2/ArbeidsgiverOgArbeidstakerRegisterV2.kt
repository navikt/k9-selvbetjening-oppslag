package no.nav.k9.utgaende.rest.aaregv2

import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.inngaende.correlationId
import no.nav.k9.inngaende.idToken
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.utgaende.rest.*
import no.nav.k9.utgaende.rest.ArbeidsforholdType.FRILANS
import no.nav.k9.utgaende.rest.NavHeaderValues
import no.nav.k9.utgaende.rest.NavHeaders
import no.nav.k9.utgaende.rest.TypeArbeidssted.Companion.somTypeArbeidssted
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import kotlin.coroutines.coroutineContext

/**
 * @see <a href="https://aareg-services.dev.intern.nav.no/swagger-ui/index.html?urls.primaryName=aareg.api.v2#/arbeidstaker/finnArbeidsforholdPrArbeidstaker">Aareg-services swagger docs</a>
 */

internal class ArbeidsgiverOgArbeidstakerRegisterV2 (
    baseUrl: URI,
    private val cachedAccessTokenClient: CachedAccessTokenClient,
    private val aaregTokenxAudience: String
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(ArbeidsgiverOgArbeidstakerRegisterV2::class.java)
    }

    private val arbeidsforholdPerArbeidstakerUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("arbeidstaker", "arbeidsforhold"),
        queryParameters = mapOf(
            "arbeidsforholdtype" to listOf(ArbeidsforholdType.values().joinToString(",") { it.type}),
            "arbeidsforholdstatus" to listOf(ArbeidsforholdStatus.values().joinToString(","))
        )
    )

    internal suspend fun arbeidsgivere(
        ident: Ident,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate
    ) : Arbeidsgivere{
        val exchangeToken = cachedAccessTokenClient.getAccessToken(
            scopes = setOf(aaregTokenxAudience),
            onBehalfOf = coroutineContext.idToken().value
        )

        val httpRequest = arbeidsforholdPerArbeidstakerUrl.toString()
            .httpGet()
            .header(
                HttpHeaders.Authorization to "Bearer ${exchangeToken.token}",
                HttpHeaders.Accept to "application/json",
                NavHeaders.CallId to coroutineContext.correlationId().value,
                NavHeaders.PersonIdent to ident.value
            )

        logger.restKall(arbeidsforholdPerArbeidstakerUrl.toString(), true)

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

        if (json.isEmpty) return Arbeidsgivere(
            organisasjoner = emptySet(),
            privateArbeidsgivere = emptySet(),
            frilansoppdrag = emptySet()
        )

        return Arbeidsgivere(
            organisasjoner = json.hentOrganisasjonerV2(fraOgMed, tilOgMed),
            privateArbeidsgivere = json.hentPrivateArbeidsgivereV2(fraOgMed, tilOgMed),
            frilansoppdrag = json.hentFrilansoppdragV2(fraOgMed, tilOgMed)
        )
    }
}

private enum class ArbeidsforholdStatus(){
    AKTIV,
    AVSLUTTET,
    FREMTIDIG
}