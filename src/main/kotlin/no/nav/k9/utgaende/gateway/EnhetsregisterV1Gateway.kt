package no.nav.k9.utgaende.gateway

import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.utgaende.rest.EnhetOrganisasjon
import no.nav.k9.utgaende.rest.EnhetOrganisasjonsnummer
import no.nav.k9.utgaende.rest.EnhetsregisterV1

internal class EnhetsregisterV1Gateway(
    private val enhetsregisterV1: EnhetsregisterV1
) {
    internal companion object {
        private val stoettedeAttributter = setOf(
            Attributt.arbeidsgivereOrganisasjonerNavn
        )
    }

    suspend internal fun organisasjon(
        organisasjonsnummer: EnhetOrganisasjonsnummer,
        attributter: Set<Attributt>
    ) : EnhetOrganisasjon? {
        if (!attributter.any { it in stoettedeAttributter }) return null
        return enhetsregisterV1.noekkelinfo(organisasjonsnummer)
    }
}