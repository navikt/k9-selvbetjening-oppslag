package no.nav.k9.utgaende.gateway

import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.utgaende.rest.Arbeidsforhold
import no.nav.k9.utgaende.rest.ArbeidsgiverOgArbeidstakerRegisterV1
import java.time.LocalDate

internal class ArbeidsgiverOgArbeidstakerRegisterV1Gateway(
    private val arbeidstakerOgArbeidstakerRegisterV1: ArbeidsgiverOgArbeidstakerRegisterV1
)  {
    internal companion object {
        private val stoettedeAttributter = setOf(
            Attributt.arbeidsgivereOrganisasjonerOrganisasjonsnummer,
            Attributt.arbeidsgivereOrganisasjonerNavn
        )
    }

    suspend internal fun arbeidsforhold(
        ident: Ident,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        attributter: Set<Attributt>
    ) : Arbeidsforhold? {
        if (!attributter.any { it in stoettedeAttributter }) return null
        return arbeidstakerOgArbeidstakerRegisterV1.arbeidsforhold(
            ident = ident,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed
        )
    }
}