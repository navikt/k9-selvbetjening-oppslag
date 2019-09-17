package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.OrganisasjonV5Gateway
import no.nav.k9.utgaende.gateway.Organisasjonsnummer
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Organisasjon

internal class ArbeidsgivereOppslag(
    private val organisasjonV5Gateway: OrganisasjonV5Gateway
) {

    internal fun organisasjoner(
        attributter: Set<Attributt>,
        arbeidsforhold: Set<Arbeidsforhold>?
    ) : Set<Organisasjon>? {
        if (!attributter.etterspurtArbeidsgibereOrganaisasjoner()) return null

        return arbeidsforhold!!
            .map {
                it.arbeidsgiver
            }
            .map { it as Organisasjon }
            .distinctBy {
                it.orgnummer
            }
            .map {
                if (it.navn == null) {
                    it.navn = hentNavn(
                        orgnummer = it.orgnummer,
                        attributter = attributter
                    )
                }
                it
        }.toSet()
    }

    private fun hentNavn(
        orgnummer: String,
        attributter: Set<Attributt>
    ) = try {
        organisasjonV5Gateway.organisasjon(
            organisasjonsnummer = Organisasjonsnummer(orgnummer),
            attributter = attributter
        )?.navn
    } catch (cause: Throwable) {
        null
    }
}

