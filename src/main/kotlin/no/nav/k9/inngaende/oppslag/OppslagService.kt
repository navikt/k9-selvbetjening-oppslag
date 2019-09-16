package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.AktoerRegisterV1Gateway
import no.nav.k9.utgaende.gateway.PersonV3Gateway
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person

internal class OppslagService(
    private val personV3Gateway: PersonV3Gateway,
    private val aktoerRegisterV1Gateway: AktoerRegisterV1Gateway
) {
    private val barnOppslag = BarnOppslag(
        personV3Gateway = personV3Gateway,
        aktoerRegisterV1Gateway = aktoerRegisterV1Gateway
    )

    internal suspend fun oppslag(
        fødselsnummer: Fødselsnummer,
        attributter: Set<Attributt>) : OppslagResultat {

        val personV3 = personV3Gateway.person(fødselsnummer, attributter)

        return OppslagResultat(
            meg = meg(
                attributter = attributter,
                fødselsnummer = fødselsnummer,
                person = personV3
            ),
            barn = barnOppslag.barn(
                attributter = attributter,
                person = personV3
            )
        )
    }

    private suspend fun meg(
        person: Person?,
        fødselsnummer: Fødselsnummer,
        attributter: Set<Attributt>
    ) : Meg? {
        return if (!attributter.etterspurtMeg()) null else {
            Meg(
                person = person,
                aktørId = aktoerRegisterV1Gateway.aktørId(
                    fødselsnummer = fødselsnummer,
                    attributter = attributter
                )
            )
        }
    }
}