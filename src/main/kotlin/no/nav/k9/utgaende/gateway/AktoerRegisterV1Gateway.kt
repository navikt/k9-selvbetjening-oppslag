package no.nav.k9.utgaende.gateway

import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.utgaende.rest.AktoerregisterV1
import no.nav.k9.utgaende.rest.AktoerId

internal class AktoerRegisterV1Gateway(
    private val aktoerRegisterV1: AktoerregisterV1
) {
    internal companion object {
        private val stoettedeAttributter = setOf(
            Attributt.aktoerId,
            Attributt.barnAktoerId
        )
    }

    internal suspend fun aktoerId(
        ident: Ident,
        attributter: Set<Attributt>
    ) : AktoerId? {
        return if (attributter.any { it in stoettedeAttributter }) {
            aktoerRegisterV1.aktoerId(ident)
        } else null
    }
}