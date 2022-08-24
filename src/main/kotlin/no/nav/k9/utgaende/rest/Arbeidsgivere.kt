package no.nav.k9.utgaende.rest

import no.nav.k9.utgaende.rest.aaregv2.TypeArbeidssted
import java.time.LocalDate

internal data class Arbeidsgivere(
    internal val organisasjoner: Set<OrganisasjonArbeidsgivere>,
    internal val privateArbeidsgivere: Set<PrivatArbeidsgiver> = emptySet(),
    internal val frilansoppdrag: Set<Frilansoppdrag> = emptySet()
)

internal data class OrganisasjonArbeidsgivere(
    internal val organisasjonsnummer: String,
    internal val ansattFom: LocalDate? = null,
    internal val ansattTom: LocalDate? = null
)

internal data class PrivatArbeidsgiver (
    internal val offentligIdent: String,
    internal val ansattFom: LocalDate,
    internal val ansattTom: LocalDate? = null
)

internal data class Frilansoppdrag (
    internal val type: TypeArbeidssted,
    internal val organisasjonsnummer: String? = null,
    internal val navn: String? = null,
    internal val offentligIdent: String? = null,
    internal val ansattFom: LocalDate,
    internal val ansattTom: LocalDate? = null
)