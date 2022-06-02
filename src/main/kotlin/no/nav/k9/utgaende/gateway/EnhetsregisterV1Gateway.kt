package no.nav.k9.utgaende.gateway

import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.utgaende.rest.Enhet
import no.nav.k9.utgaende.rest.EnhetsregisterV1

internal class EnhetsregisterV1Gateway(
    private val enhetsregisterV1: EnhetsregisterV1
) {
    internal companion object {
        private val støttedeAttributter = setOf(
            Attributt.arbeidsgivereOrganisasjonerNavn
        )
    }

    internal suspend fun enhet(
        organisasjonsnummer: String,
        attributter: Set<Attributt>
    ) : Enhet? {
        if (!attributter.any { it in støttedeAttributter }) return null
        return enhetsregisterV1.nøkkelinfo(organisasjonsnummer)
    }
}
