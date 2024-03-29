package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.EnhetsregisterV1Gateway
import no.nav.k9.utgaende.rest.Arbeidsgivere
import java.time.LocalDate

internal class ArbeidsgivereOppslag(
    private val enhetsregisterV1Gateway: EnhetsregisterV1Gateway,
) {

    internal suspend fun organisasjoner(
        attributter: Set<Attributt>,
        arbeidsgivere: Arbeidsgivere?,
    ): Set<ArbeidsgiverOrganisasjon>? {

        if (!attributter.etterspurtArbeidsgivereOrganisasjoner()) return null

        return arbeidsgivere!!.organisasjoner.map {
            ArbeidsgiverOrganisasjon(
                organisasjonsnummer = it.organisasjonsnummer,
                navn = hentNavn(
                    organisasjonsnummer = it.organisasjonsnummer,
                    attributter = attributter
                ),
                ansattFom = it.ansattFom,
                ansattTom = it.ansattTom
            )
        }.toSet()

    }

    suspend fun hentNavn(
        organisasjonsnummer: String,
        attributter: Set<Attributt>,
    ) = try {
        enhetsregisterV1Gateway.enhet(
            attributter = attributter,
            organisasjonsnummer = organisasjonsnummer
        )?.navn
    } catch (cause: Throwable) {
        null
    }
}

internal data class ArbeidsgiverOrganisasjon(
    internal val organisasjonsnummer: String,
    internal val navn: String?,
    internal val ansattFom: LocalDate? = null,
    internal val ansattTom: LocalDate? = null,
)
