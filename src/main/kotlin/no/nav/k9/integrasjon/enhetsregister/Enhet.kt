package no.nav.k9.integrasjon.enhetsregister

import java.time.LocalDate

internal data class Enhet(
    internal val organisasjonsnummer: String,
    internal val navn: String?,
    internal val enhetstype: String?,
    internal val opphørsdato: LocalDate?
)

