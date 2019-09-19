package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.*
import no.nav.k9.utgaende.gateway.AktoerRegisterV1Gateway
import no.nav.k9.utgaende.gateway.ArbeidsgiverOgArbeidstakerRegisterV1Gateway
import no.nav.k9.utgaende.gateway.PersonV3Gateway
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import java.time.LocalDate

internal class OppslagService(
    private val personV3Gateway: PersonV3Gateway,
    private val aktoerRegisterV1Gateway: AktoerRegisterV1Gateway,
    private val arbeidsgiverOgArbeidstakerRegisterV1Gateway: ArbeidsgiverOgArbeidstakerRegisterV1Gateway,
    private val enhetsregisterV1Gateway: EnhetsregisterV1Gateway
) {
    private val barnOppslag = BarnOppslag(
        personV3Gateway = personV3Gateway,
        aktoerRegisterV1Gateway = aktoerRegisterV1Gateway
    )

    private val arbeidsgiverOppslag = ArbeidsgivereOppslag(
        enhetsregisterV1Gateway = enhetsregisterV1Gateway
    )

    internal suspend fun oppslag(
        ident: Ident,
        attributter: Set<Attributt>,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate) : OppslagResultat {

        val personV3 = personV3Gateway.person(
            Ident = ident,
            attributter = attributter
        )

        val arbeidsforhold = arbeidsgiverOgArbeidstakerRegisterV1Gateway.arbeidsforhold(
            ident = ident,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed,
            attributter = attributter
        )

        return OppslagResultat(
            meg = meg(
                attributter = attributter,
                ident = ident,
                person = personV3
            ),
            barn = barnOppslag.barn(
                attributter = attributter,
                person = personV3
            ),
            arbeidsgivereOrganisasjoner = arbeidsgiverOppslag.organisasjoner(
                attributter = attributter,
                arbeidsforhold = arbeidsforhold
            )
        )
    }

    private suspend fun meg(
        person: Person?,
        ident: Ident,
        attributter: Set<Attributt>
    ) : Meg? {
        return if (!attributter.etterspurtMeg()) null else {
            Meg(
                person = person,
                aktørId = aktoerRegisterV1Gateway.aktørId(
                    ident = ident,
                    attributter = attributter
                )
            )
        }
    }
}