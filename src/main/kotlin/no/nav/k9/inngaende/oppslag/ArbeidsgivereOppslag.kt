package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.EnhetsregisterV1Gateway
import no.nav.k9.utgaende.rest.Arbeidsforhold

internal class ArbeidsgivereOppslag(
    private val enhetsregisterV1Gateway: EnhetsregisterV1Gateway
) {

    internal suspend fun organisasjoner(
        attributter: Set<Attributt>,
        arbeidsforhold: Arbeidsforhold?
    ) : Set<ArbeidsgiverOrganisasjon>? {

        if (!attributter.etterspurtArbeidsgibereOrganaisasjoner()) return null

        return arbeidsforhold!!.organisasjoner.map {
            ArbeidsgiverOrganisasjon(
                organisasjonsnummer = it.organisasjonsnummer,
                navn = hentNavn(
                    organisasjonsnummer = it.organisasjonsnummer,
                    attributter = attributter
                )
            )
        }.toSet()

    }

    private suspend fun hentNavn(
        organisasjonsnummer: String,
        attributter: Set<Attributt>
    ) = try {
        enhetsregisterV1Gateway.organisasjon(
            attributter = attributter,
            organisasjonsnummer = organisasjonsnummer
        )?.navn
    } catch (cause: Throwable) {
        null
    }
}

internal data class ArbeidsgiverOrganisasjon(
    internal val organisasjonsnummer: String,
    internal val navn: String?
)
