package no.nav.k9.utgaende.gateway

import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.utgaende.rest.Arbeidsgivere
import no.nav.k9.utgaende.rest.aaregv2.ArbeidsgiverOgArbeidstakerRegisterV2
import java.time.LocalDate

internal class ArbeidsgiverOgArbeidstakerRegisterGateway(
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

    internal suspend fun arbeidsgivere(
        ident: Ident,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
        inkluderAlleAnsettelsesperioder: Boolean,
        attributter: Set<Attributt>
    ): Arbeidsgivere? {
        if (!attributter.any { it in støttedeAttributter }) return null
        return arbeidstakerOgArbeidstakerRegisterV2.arbeidsgivere(
            ident = ident,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed,
            inkluderAlleAnsettelsesperioder
        )
    }
}
