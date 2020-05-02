package no.nav.k9.utgaende.gateway

import io.prometheus.client.Counter
import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.utgaende.rest.BrregProxyV1
import no.nav.k9.utgaende.rest.Foretak

internal class BrregProxyV1Gateway (
    private val brregProxyV1: BrregProxyV1
) {

    private companion object {
        private val støttedeAttributter = setOf(
            Attributt.personligForetakOrganisasjonsnummer,
            Attributt.personligForetakRegistreringsdato,
            Attributt.personligForetakNavn,
            Attributt.personligForetakOpphørsdato
        )

        private val rolleCounter = Counter
            .build("brreg_roller", "Roller fra oppslag mot Brønnøysundregisteret.")
            .labelNames("rolle")
            .register()
    }

    internal suspend fun foretak(
        ident: Ident,
        attributter: Set<Attributt>
    ) : Set<Foretak>? {
        if (!attributter.any { it in støttedeAttributter }) return null
        val foretak = brregProxyV1.foretak(ident)
        foretak
            .flatMap { it.rollebeskrivelser }
            .map { it.metricsFriendly() }
            .forEach { rolleCounter.labels(it).inc() }
        return foretak
    }
}