package no.nav.k9.utgaende.rest

import no.nav.k9.inngaende.oppslag.Ident
import java.time.LocalDate

internal class BrregProxyV1 {
    internal suspend fun foretak(
        ident: Ident
    ) : Set<Foretak> {
        return setOf()
    }
}

internal data class Foretak(
    val organisasjonsnummer: String,
    val registreringsdato: LocalDate,
    val rollebeskrivelse: String
)