package no.nav.k9.utgaende.gateway

import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Fødselsnummer
import no.nav.k9.utgaende.rest.Arbeidsforhold
import no.nav.k9.utgaende.rest.ArbeidsgiverOgArbeidstakerRegisterV1
import java.time.LocalDate

internal class ArbeidsgiverOgArbeidstakerRegisterV1Gateway(
    private val arbeidstakerOgArbeidstakerRegisterV1: ArbeidsgiverOgArbeidstakerRegisterV1
)  {
    internal companion object {
        private val støttedeAttributter = setOf(
            Attributt.arbeidsgivereOrganisasjonerOrganisasjonsnummer,
            Attributt.arbeidsgivereOrganisasjonerNavn
        )
    }

    suspend internal fun arbeidsforhold(
        fødselsnummer: Fødselsnummer,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        attributter: Set<Attributt>
    ) : Arbeidsforhold? {
        if (!attributter.any { it in støttedeAttributter }) return null
        return arbeidstakerOgArbeidstakerRegisterV1.arbeidsforhold(
            fødselsnummer = fødselsnummer,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed
        )
    }
}