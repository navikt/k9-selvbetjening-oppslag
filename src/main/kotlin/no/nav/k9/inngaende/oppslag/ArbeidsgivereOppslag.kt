package no.nav.k9.inngaende.oppslag

import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Arbeidsforhold
import no.nav.tjeneste.virksomhet.arbeidsforhold.v3.informasjon.arbeidsforhold.Organisasjon

internal class ArbeidsgivereOppslag {

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
                if (attributter.contains(Attributt.arbeidsgivereOrganisasjonerNavn) && it.navn == null) {
                    it.navn = "Todo"
                }
                it
        }.toSet()
    }
}

