package no.nav.k9.utgaende.gateway

import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.utgaende.rest.Arbeidsgivere
import no.nav.k9.utgaende.rest.ArbeidsgiverOgArbeidstakerRegisterV1
import no.nav.k9.utgaende.rest.aaregv2.ArbeidsgiverOgArbeidstakerRegisterV2
import java.time.LocalDate

internal class ArbeidsgiverOgArbeidstakerRegisterV1Gateway(
    private val arbeidstakerOgArbeidstakerRegisterV1: ArbeidsgiverOgArbeidstakerRegisterV1,
    private val arbeidstakerOgArbeidstakerRegisterV2: ArbeidsgiverOgArbeidstakerRegisterV2

)  {
    internal companion object {
        private val støttedeAttributter = setOf(
            Attributt.arbeidsgivereOrganisasjonerOrganisasjonsnummer,
            Attributt.arbeidsgivereOrganisasjonerNavn,
            Attributt.privateArbeidsgivereAnsettelseperiode,
            Attributt.privateArbeidsgivereOffentligIdent,
            Attributt.frilansoppdrag
        )
    }

    suspend internal fun arbeidsgivere(
        ident: Ident,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        attributter: Set<Attributt>
    ) : Arbeidsgivere? {
        if (!attributter.any { it in støttedeAttributter }) return null
        return arbeidstakerOgArbeidstakerRegisterV1.arbeidsgivere(
            ident = ident,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed
        )
    }

    suspend internal fun arbeidsgivereV2(
        ident: Ident,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        attributter: Set<Attributt>
    ) {
        if (!attributter.any { it in støttedeAttributter }) return
        arbeidstakerOgArbeidstakerRegisterV2.arbeidsgivere(
            ident = ident,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed
        )
    }
}