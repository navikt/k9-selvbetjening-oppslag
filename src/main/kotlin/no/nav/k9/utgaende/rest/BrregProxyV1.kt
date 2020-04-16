package no.nav.k9.utgaende.rest

import no.nav.k9.inngaende.oppslag.Ident
import java.time.LocalDate

/**
 * Hovedstatus og Understatuser:
 * https://www.brreg.no/produkter-og-tjenester/bestille-produkter/maskinlesbare-data-enhetsregisteret/full-tilgang-enhetsregisteret/teknisk-dokumentasjon-for-maskinell-tilgang-til-enhetsregisteret/grunndataws/
 *
 */

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
    val rolle: String
)