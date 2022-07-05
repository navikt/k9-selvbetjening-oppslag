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

        val organisasjoner = json.hentOrganisasjoner(fraOgMed, tilOgMed)
        val privateArbeidsgivere = json.hentPrivateArbeidsgivere(fraOgMed, tilOgMed)
        val frilansoppdrag = json.hentFrilansoppdrag(fraOgMed, tilOgMed)

        return Arbeidsgivere(
            organisasjoner = organisasjoner,
            privateArbeidsgivere = privateArbeidsgivere,
            frilansoppdrag = frilansoppdrag
        )
    }
}

private fun JSONArray.hentFrilansoppdrag(fraOgMed: LocalDate, tilOgMed: LocalDate): Set<Frilansoppdrag> =
    hentArbeidsgivereMedAnsettelseperiode()
        .filter { it.erFrilansaktivitet() }
        .map { ansettelsesforhold ->
            val (ansattFom, ansattTom) = ansettelsesforhold.hentFomTomFraAnsettelseperiode()
            val typeArbeidssted = ansettelsesforhold.hentArbeidsstedType()

            val offentligIdent = if (typeArbeidssted == "Person") ansettelsesforhold.hentØnsketIdentFraArbeidssted("FOLKEREGISTERIDENT") else null
            val organisasjonsnummer = if (typeArbeidssted == "Underenhet") ansettelsesforhold.hentØnsketIdentFraArbeidssted("ORGANISASJONSNUMMER") else null

            Frilansoppdrag(
                type = typeArbeidssted.somTypeArbeidssted(),
                organisasjonsnummer = organisasjonsnummer,
                navn = null,
                offentligIdent = offentligIdent,
                ansattFom = LocalDate.parse(ansattFom),
                ansattTom = ansattTom?.let { LocalDate.parse(it) }
            )
        }
        .filter { erAnsattIPerioden(it.ansattFom, it.ansattTom, fraOgMed, tilOgMed) }
        .toSet()


private fun JSONArray.hentPrivateArbeidsgivere(fraOgMed: LocalDate, tilOgMed: LocalDate): Set<PrivatArbeidsgiver> =
    hentArbeidsgivereMedAnsettelseperiode()
        .filterNot { it.erFrilansaktivitet() }
        .filter { it.arbeidstedErPerson() }
        .map { ansettelsesforhold ->
            val ident = ansettelsesforhold.hentØnsketIdentFraArbeidssted("FOLKEREGISTERIDENT")
            val (ansattFom, ansattTom) = ansettelsesforhold.hentFomTomFraAnsettelseperiode()

            PrivatArbeidsgiver(
                offentligIdent = ident,
                ansattFom = LocalDate.parse(ansattFom),
                ansattTom = ansattTom?.let { LocalDate.parse(it) }
            )
        }
        .filter { erAnsattIPerioden(it.ansattFom, it.ansattTom, fraOgMed, tilOgMed) }
        .distinctBy { it.offentligIdent }
        .toSet()

private fun JSONObject.hentØnsketIdentFraArbeidssted(identType: String): String {
    val identer = this.getJSONObject("arbeidssted").getJSONArray("identer")
    if(identer.length() == 1) return identer.getJSONObject(0).getString("ident")
    identer.forEach {
        it as JSONObject
        if(it.getString("type").equals(identType)) return it.getString("ident")
    }
    return identer.getJSONObject(0).getString("ident")
}

private fun JSONArray.hentOrganisasjoner(fraOgMed: LocalDate, tilOgMed: LocalDate): Set<OrganisasjonArbeidsgivere> =
    hentArbeidsgivereMedAnsettelseperiode()
        .filterNot { it.erFrilansaktivitet() }
        .filter { it.arbeidstedErUnderenhet() }
        .map { ansettelsesforhold ->
            val organisasjonsnummer = ansettelsesforhold.hentØnsketIdentFraArbeidssted("ORGANISASJONSNUMMER")
            val (ansattFom, ansattTom) = ansettelsesforhold.hentFomTomFraAnsettelseperiode()

            OrganisasjonArbeidsgivere(
                organisasjonsnummer = organisasjonsnummer,
                ansattFom = LocalDate.parse(ansattFom),
                ansattTom = ansattTom?.let { LocalDate.parse(it) }
            )
        }
        .filter { erAnsattIPerioden(it.ansattFom, it.ansattTom, fraOgMed, tilOgMed) }
        .distinctBy { it.organisasjonsnummer }
        .toSet()

private fun JSONObject.erFrilansaktivitet() = getJSONObject("type").getString("kode").equals(FRILANS.type)
private fun JSONObject.hentArbeidsstedType() = getJSONObject("arbeidssted").getString("type")
private fun JSONObject.arbeidstedErUnderenhet() = hentArbeidsstedType().equals("Underenhet")
private fun JSONObject.arbeidstedErPerson() = hentArbeidsstedType().equals("Person")

private fun JSONArray.hentArbeidsgivereMedAnsettelseperiode(): Sequence<JSONObject> = this
    .asSequence()
    .map { it as JSONObject }
    .filter { it.has("arbeidssted") }
    .filter { it.has("ansettelsesperiode") && it.getJSONObject("ansettelsesperiode").has("startdato") }

private fun JSONObject.hentFomTomFraAnsettelseperiode(): Pair<String, String?> {
    val ansettelsesperiode = this.getJSONObject("ansettelsesperiode")
    return Pair(ansettelsesperiode.getString("startdato"), ansettelsesperiode.getStringOrNull("sluttdato"))
}