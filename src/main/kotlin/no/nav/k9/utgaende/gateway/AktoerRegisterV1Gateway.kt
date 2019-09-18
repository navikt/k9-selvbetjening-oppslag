package no.nav.k9.utgaende.gateway

import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Fødselsnummer
import no.nav.k9.utgaende.rest.AktørId
import no.nav.k9.utgaende.rest.AktørRegisterV1

internal class AktoerRegisterV1Gateway(
    private val aktørRegisterV1: AktørRegisterV1
) {
    internal companion object {
        private val støttedeAttributter = setOf(
            Attributt.aktørId,
            Attributt.barnAktørId
        )
    }

    internal suspend fun aktørId(
        fødselsnummer: Fødselsnummer,
        attributter: Set<Attributt>
    ) : AktørId? {
        return if (attributter.any { it in støttedeAttributter }) {
            aktørRegisterV1.aktørId(fødselsnummer)
        } else null
    }
}