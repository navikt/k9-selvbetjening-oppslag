package no.nav.k9.integrasjon.aareg

import kotlinx.coroutines.currentCoroutineContext
import no.nav.k9.inngaende.correlationId
import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.integrasjon.common.logResponse
import no.nav.k9.integrasjon.common.restKall
import no.nav.k9.integrasjon.common.somJsonArray
import org.json.JSONArray
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.time.LocalDate

/**
 * @see <a href="https://aareg-services.dev.intern.nav.no/swagger-ui/index.html?urls.primaryName=aareg.api.v2#/arbeidstaker/finnArbeidsforholdPrArbeidstaker">Aareg-services swagger docs</a>
 */
@Service
internal class AaregService(
    private val aaregRetryClient: AaregRetryClient,
    private val aaregAuthService: AaregAuthService,
    @Value("\${nav.register-urls.arbeidsgiver-og-arbeidstaker-v2}") baseUrl: URI,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(AaregService::class.java)
        private const val ARBEIDSFORHOLD_PATH =
            "/arbeidstaker/arbeidsforhold?arbeidsforholdtype={arbeidsforholdtype}&arbeidsforholdstatus={arbeidsforholdstatus}"

        private val støttedeAttributter = setOf(
            Attributt.arbeidsgivereOrganisasjonerOrganisasjonsnummer,
            Attributt.arbeidsgivereOrganisasjonerNavn,
            Attributt.privateArbeidsgivereAnsettelseperiode,
            Attributt.privateArbeidsgivereOffentligIdent,
            Attributt.frilansoppdrag
        )
    }

    private val baseUrl = baseUrl.toString().trimEnd('/')
    private val queryVariabler = mapOf(
        "arbeidsforholdtype" to ArbeidsforholdType.values().joinToString(",") { it.type },
        "arbeidsforholdstatus" to ArbeidsforholdStatus.somQueryParameters()
    )

    internal suspend fun arbeidsgivere(
        ident: Ident,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        inkluderAlleAnsettelsesperioder: Boolean,
        attributter: Set<Attributt>
    ): Arbeidsgivere? {
        if (!attributter.any { it in støttedeAttributter }) return null

        val exchangeToken = aaregAuthService.borgerToken()
        val callId = currentCoroutineContext().correlationId().value

        logger.restKall("$baseUrl$ARBEIDSFORHOLD_PATH", true)

        val json = aaregRetryClient.arbeidsforhold(ARBEIDSFORHOLD_PATH, queryVariabler, exchangeToken, callId, ident)
            .somJsonArray()

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
