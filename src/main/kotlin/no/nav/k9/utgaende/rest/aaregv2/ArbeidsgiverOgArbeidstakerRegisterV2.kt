package no.nav.k9.utgaende.rest.aaregv2

import kotlinx.coroutines.currentCoroutineContext
import no.nav.k9.inngaende.correlationId
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.utgaende.auth.AaregAuthService
import no.nav.k9.utgaende.rest.*
import org.json.JSONArray
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.retry.support.RetryTemplate
import org.springframework.web.client.RestClient
import java.net.URI
import java.time.LocalDate

/**
 * @see <a href="https://aareg-services.dev.intern.nav.no/swagger-ui/index.html?urls.primaryName=aareg.api.v2#/arbeidstaker/finnArbeidsforholdPrArbeidstaker">Aareg-services swagger docs</a>
 */

internal class ArbeidsgiverOgArbeidstakerRegisterV2 (
    baseUrl: URI,
    restClientBuilder: RestClient.Builder,
    private val retryTemplate: RetryTemplate,
    private val aaregAuthService: AaregAuthService,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(ArbeidsgiverOgArbeidstakerRegisterV2::class.java)
        private const val ARBEIDSFORHOLD_PATH =
            "/arbeidstaker/arbeidsforhold?arbeidsforholdtype={arbeidsforholdtype}&arbeidsforholdstatus={arbeidsforholdstatus}"
    }

    private val baseUrl = baseUrl.toString().trimEnd('/')
    private val restClient = restClientBuilder.baseUrl(this.baseUrl).build()
    private val queryVariabler = mapOf(
        "arbeidsforholdtype" to ArbeidsforholdType.values().joinToString(",") { it.type },
        "arbeidsforholdstatus" to ArbeidsforholdStatus.somQueryParameters()
    )

    internal suspend fun arbeidsgivere(
        ident: Ident,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        inkluderAlleAnsettelsesperioder: Boolean
    ) : Arbeidsgivere{
        val exchangeToken = aaregAuthService.borgerToken()
        val callId = currentCoroutineContext().correlationId().value

        logger.restKall("$baseUrl$ARBEIDSFORHOLD_PATH", true)

        val json = retryTemplate.execute<JSONArray, RuntimeException> {
            restClient.get()
                .uri(ARBEIDSFORHOLD_PATH, queryVariabler)
                .headers { it.setBearerAuth(exchangeToken) }
                .accept(MediaType.APPLICATION_JSON)
                .header(NavHeaders.CallId, callId)
                .header(NavHeaders.PersonIdent, ident.value)
                .retrieve()
                .body(String::class.java)
                ?.somJsonArray()
                ?: throw IllegalStateException("Tom respons ved henting av arbeidsforhold per arbeidstaker")
        }

        logger.logResponse(json)

        if (json.isEmpty) return Arbeidsgivere(
            organisasjoner = emptyList(),
            privateArbeidsgivere = emptySet(),
            frilansoppdrag = emptySet()
        )

        return Arbeidsgivere(
            organisasjoner = json.hentOrganisasjonerV2(fraOgMed, tilOgMed, inkluderAlleAnsettelsesperioder),
            privateArbeidsgivere = json.hentPrivateArbeidsgivereV2(fraOgMed, tilOgMed),
            frilansoppdrag = json.hentFrilansoppdragV2(fraOgMed, tilOgMed)
        )
    }
}

enum class ArbeidsforholdType(val type: String){
    ORDINÆRT("ordinaertArbeidsforhold"),
    MARITIMT("maritimtArbeidsforhold"),
    FORENKLET("forenkletOppgjoersordning"),
    FRILANS("frilanserOppdragstakerHonorarPersonerMm")
}


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
