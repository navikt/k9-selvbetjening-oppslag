package no.nav.k9.integrasjon.aareg

import no.nav.k9.inngaende.currentCorrelationId
import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.integrasjon.common.logResponse
import no.nav.k9.integrasjon.common.restKall
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.time.LocalDate

/**
 * @see <a href="https://aareg-services.dev.intern.nav.no/swagger-ui/index.html?urls.primaryName=aareg.api.v2#/arbeidstaker/finnArbeidsforholdPrArbeidstaker">Aareg-services swagger docs</a>
 */
@Service
internal class AaregService(
    private val aaregRetryClient: AaregRetryClient,
    private val aaregAuthService: AaregAuthService,
    private val jsonMapper: JsonMapper,
    @Value("\${nav.register-urls.arbeidsgiver-og-arbeidstaker-v2}") baseUrl: URI,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(AaregService::class.java)

        private val støttedeAttributter = setOf(
            Attributt.arbeidsgivereOrganisasjonerOrganisasjonsnummer,
            Attributt.arbeidsgivereOrganisasjonerNavn,
            Attributt.privateArbeidsgivereAnsettelseperiode,
            Attributt.privateArbeidsgivereOffentligIdent,
            Attributt.frilansoppdrag
        )
    }

    private val baseUrl = baseUrl.toString().trimEnd('/')

    internal fun arbeidsgivere(
        ident: Ident,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        inkluderAlleAnsettelsesperioder: Boolean,
        attributter: Set<Attributt>
    ): Arbeidsgivere? {
        if (!attributter.any { it in støttedeAttributter }) return null

        val exchangeToken = aaregAuthService.borgerToken()
        val callId = currentCorrelationId().value

        logger.restKall("$baseUrl$ARBEIDSFORHOLD_PATH")

        val respons = aaregRetryClient.arbeidsforhold(exchangeToken, callId, ident)
        // Aareg kan returnere duplikate nøkler. Jackson bruker da den siste verdien i stedet for å feile.
        val json = jsonMapper.readTree(respons)
        check(json.isArray) { "Forventet en liste med arbeidsforhold fra aareg." }

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
    FRILANS("frilanserOppdragstakerHonorarPersonerMm");

    companion object {
        internal fun somQueryParameters() = entries.joinToString(",") { it.type }
    }
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
