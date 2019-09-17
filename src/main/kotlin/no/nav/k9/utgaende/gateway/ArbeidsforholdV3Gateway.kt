package no.nav.k9.utgaende.gateway

import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Fødselsnummer
import no.nav.k9.utgaende.ws.toXmlGregorianCalendar
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.binding.ArbeidsforholdV3
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.NorskIdent
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Periode
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Regelverker
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.meldinger.FinnArbeidsforholdPrArbeidstakerRequest
import java.time.LocalDate

internal class ArbeidsforholdV3Gateway(
    private val arbeidsforholdV3: ArbeidsforholdV3
) {
    internal companion object {
        private const val A_ORDNINGEN = "A_ORDNINGEN"
        private val støttedeAttributter = setOf(
            Attributt.arbeidsgivereOrganisasjonerOrganisasjonsnummer,
            Attributt.arbeidsgivereOrganisasjonerNavn
        )
    }

    internal fun arbeidsforhold(
        fødselsnummer: Fødselsnummer,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        attributter: Set<Attributt>
    ) : Set<Arbeidsforhold>? {
        if (!attributter.any {it in støttedeAttributter}) return null

        val request = FinnArbeidsforholdPrArbeidstakerRequest().apply {
            ident = NorskIdent().apply {
                ident = fødselsnummer.value
            }
            arbeidsforholdIPeriode = Periode().apply {
                this.fom = fraOgMed.toXmlGregorianCalendar()
                this.tom = tilOgMed.toXmlGregorianCalendar()
            }
            rapportertSomRegelverk = Regelverker().apply {
                value = A_ORDNINGEN
                kodeRef = A_ORDNINGEN
            }
        }

        return arbeidsforholdV3.finnArbeidsforholdPrArbeidstaker(request).arbeidsforhold.toSet()
    }
}