package no.nav.k9.utgaende.gateway

import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.utgaende.rest.AktoerregisterV1
import no.nav.k9.utgaende.rest.AktørId

internal class AktoerRegisterV1Gateway(
    private val aktørRegisterV1: AktoerregisterV1
) {
    internal companion object {
        private val støttedeAttributter = setOf(
            Attributt.aktørId,
            Attributt.barnAktørId
        )
    }

    internal suspend fun aktørId(
        ident: Ident,
        attributter: Set<Attributt>
    ) : AktørId? {
        return if (attributter.any { it in støttedeAttributter }) {
            aktørRegisterV1.aktørId(ident)
        } else null
    }
}