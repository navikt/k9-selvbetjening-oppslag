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
import no.nav.k9.utgaende.rest.ArbeidsforholdType.*
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
 * @see <a href="https://aareg-services.dev.intern.nav.no/swagger-ui/index.html?urls.primaryName=aareg.api.v2">Aareg-services swagger docs</a>
 */

enum class ArbeidsforholdType(val type: String){
    ORDINÆRT("ordinaertArbeidsforhold"),
    MARITIMT("maritimtArbeidsforhold"),
    FORENKLET("forenkletOppgjoersordning"),
    FRILANS("frilanserOppdragstakerHonorarPersonerMm")
}

internal class ArbeidsgiverOgArbeidstakerRegisterV1 (
    baseUrl: URI,
    private val cachedAccessTokenClient: CachedAccessTokenClient,
    private val aaregTokenxAudience: String
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(ArbeidsgiverOgArbeidstakerRegisterV1::class.java)
    }

    private val arbeidsforholdPerArbeidstakerUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("arbeidstaker", "arbeidsforhold")
    )

    private fun urlMedFraOgMedTilOgMed(url: URI, fraOgMed: LocalDate, tilOgMed: LocalDate) = Url.buildURL(
        baseUrl = url,
        queryParameters = mapOf(
            "ansettelsesperiodeFom" to listOf(fraOgMed.toString()),
            "ansettelsesperiodeTom" to listOf(tilOgMed.toString()),
            "arbeidsforholdtype" to listOf("${ORDINÆRT.type},${MARITIMT.type},${FORENKLET.type},${FRILANS.type}")
        )
    ).toString()

    internal suspend fun arbeidsgivere(
        ident: Ident,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate
    ) : Arbeidsgivere {
        val exchangeToken = cachedAccessTokenClient.getAccessToken(
            scopes = setOf(aaregTokenxAudience),
            onBehalfOf = coroutineContext.idToken().value
        )

        val url = urlMedFraOgMedTilOgMed(arbeidsforholdPerArbeidstakerUrl, fraOgMed, tilOgMed)

        val httpRequest = urlMedFraOgMedTilOgMed(arbeidsforholdPerArbeidstakerUrl, fraOgMed, tilOgMed)
            .httpGet()
            .header(
                HttpHeaders.Authorization to "Bearer ${exchangeToken.token}",
                HttpHeaders.Accept to "application/json",
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

        if (json.isEmpty) return Arbeidsgivere(
            organisasjoner = emptySet(),
            privateArbeidsgivere = emptySet(),
            frilansoppdrag = emptySet()
        )

        val organisasjoner = json.hentOrganisasjoner()

        val privateArbeidsgivere = json.hentPrivateArbeidsgivere()

        val frilansoppdrag = json.hentFrilansoppdrag()

        return Arbeidsgivere(
            organisasjoner = organisasjoner,
            privateArbeidsgivere = privateArbeidsgivere,
            frilansoppdrag = frilansoppdrag
        )
    }
}

private fun JSONArray.hentFrilansoppdrag(): Set<Frilansoppdrag> {
    return this
        .hentArbeidsgivereMedAnsettelseperiode()
        .filter { it.getString("type").equals(FRILANS.type) }
        .map { ansettelsesforhold ->
            val (ansattFom, ansattTom) = ansettelsesforhold.hentFomTomFraAnsettelseperiode()
            val arbeidsgiver = ansettelsesforhold.getJSONObject("arbeidsgiver")
            val type = arbeidsgiver.getString("type")
            val offentligIdent = if(type == "Person") arbeidsgiver.getString("offentligIdent") else null
            val organisasjonsnummer = if(type == "Organisasjon") arbeidsgiver.getString("organisasjonsnummer") else null

            Frilansoppdrag(
                type = type.somTypeArbeidssted(),
                offentligIdent = offentligIdent,
                organisasjonsnummer = organisasjonsnummer,
                ansattFom = LocalDate.parse(ansattFom),
                ansattTom = ansattTom?.let { LocalDate.parse(it) }
            )
        }
        .toSet()
}

private fun JSONArray.hentOrganisasjoner(): Set<OrganisasjonArbeidsgivere>{
    return this
        .hentArbeidsgivereMedAnsettelseperiode()
        .filterNot { it.getString("type").equals(FRILANS.type) }
        .filter { it.getJSONObject("arbeidsgiver").has("organisasjonsnummer") }
        .map { ansettelsesforhold ->
            val organisasjonsnummer = ansettelsesforhold.getJSONObject("arbeidsgiver").getString("organisasjonsnummer")
            val (ansattFom, ansattTom) = ansettelsesforhold.hentFomTomFraAnsettelseperiode()

            OrganisasjonArbeidsgivere(
                organisasjonsnummer = organisasjonsnummer,
                ansattFom = LocalDate.parse(ansattFom),
                ansattTom = ansattTom?.let { LocalDate.parse(it) }
            )
        }
        .distinctBy { it.organisasjonsnummer }
        .toSet()
}

private fun JSONArray.hentPrivateArbeidsgivere(): Set<PrivatArbeidsgiver> {
    return this
        .hentArbeidsgivereMedAnsettelseperiode()
        .filterNot { it.getString("type").equals(FRILANS.type) }
        .filter { it.getJSONObject("arbeidsgiver").has("offentligIdent") }
        .map { ansettelsesforhold ->
            val offentligIdent = ansettelsesforhold.getJSONObject("arbeidsgiver").getString("offentligIdent")
            val (ansattFom, ansattTom) = ansettelsesforhold.hentFomTomFraAnsettelseperiode()

            PrivatArbeidsgiver(
                offentligIdent = offentligIdent,
                ansattFom = LocalDate.parse(ansattFom),
                ansattTom = ansattTom?.let { LocalDate.parse(it) }
            )
        }
        .distinctBy { it.offentligIdent }
        .toSet()
}

private fun JSONArray.hentArbeidsgivereMedAnsettelseperiode(): Sequence<JSONObject> = this
    .asSequence()
    .map { it as JSONObject }
    .filter { it.has("arbeidsgiver") }
    .filter { it.has("ansettelsesperiode") && it.getJSONObject("ansettelsesperiode").has("periode") }

private fun JSONObject.hentFomTomFraAnsettelseperiode(): Pair<String, String?> {
    val ansettelsesperiode = this.getJSONObject("ansettelsesperiode").getJSONObject("periode")
    val ansattFom = ansettelsesperiode.getString("fom")
    val ansattTom = ansettelsesperiode.getStringOrNull("tom")
    return Pair(ansattFom, ansattTom)
}

internal data class OrganisasjonArbeidsgivere(
    internal val organisasjonsnummer: String,
    internal val ansattFom: LocalDate? = null,
    internal val ansattTom: LocalDate? = null
)

internal data class PrivatArbeidsgiver (
    internal val offentligIdent: String,
    internal val ansattFom: LocalDate? = null,
    internal val ansattTom: LocalDate? = null
)

internal data class Frilansoppdrag (
    internal val type: TypeArbeidssted,
    internal val organisasjonsnummer: String? = null,
    internal val navn: String? = null,
    internal val offentligIdent: String? = null,
    internal val ansattFom: LocalDate? = null,
    internal val ansattTom: LocalDate? = null
)

internal enum class TypeArbeidssted{
    Person,
    Organisasjon;

    companion object{
        internal fun String.somTypeArbeidssted() = when(this){
            "Person" -> Person
            "Organisasjon", "Underenhet" -> Organisasjon
            else -> throw Exception("Ukjent type arbeidssted. '$this'")
        }
    }
}

internal data class Arbeidsgivere(
    internal val organisasjoner: Set<OrganisasjonArbeidsgivere>,
    internal val privateArbeidsgivere: Set<PrivatArbeidsgiver> = emptySet(),
    internal val frilansoppdrag: Set<Frilansoppdrag> = emptySet()
)