package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.*
import no.nav.k9.utgaende.gateway.AktoerRegisterV1Gateway
import no.nav.k9.utgaende.gateway.ArbeidsgiverOgArbeidstakerRegisterV1Gateway
import java.time.LocalDate

internal class OppslagService(
    private val arbeidsgiverOgArbeidstakerRegisterV1Gateway: ArbeidsgiverOgArbeidstakerRegisterV1Gateway,
    aktoerRegisterV1Gateway: AktoerRegisterV1Gateway,
    enhetsregisterV1Gateway: EnhetsregisterV1Gateway,
    tpsProxyV1Gateway: TpsProxyV1Gateway,
    brregProxyV1Gateway: BrregProxyV1Gateway
) {

    private val megOppslag = MegOppslag(
        aktoerRegisterV1Gateway = aktoerRegisterV1Gateway,
        tpsProxyV1Gateway = tpsProxyV1Gateway
    )

    private val barnOppslag = BarnOppslag(
        aktoerRegisterV1Gateway = aktoerRegisterV1Gateway,
        tpsProxyV1Gateway = tpsProxyV1Gateway
    )

    private val arbeidsgiverOppslag = ArbeidsgivereOppslag(
        enhetsregisterV1Gateway = enhetsregisterV1Gateway
    )

    private val personligeForetakOppslag = PersonligeForetakOppslag(
        enhetsregisterV1Gateway = enhetsregisterV1Gateway,
        brregProxyV1Gateway = brregProxyV1Gateway
    )

    internal suspend fun oppslag(
        ident: Ident,
        attributter: Set<Attributt>,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate) : OppslagResultat {

        val arbeidsforhold = arbeidsgiverOgArbeidstakerRegisterV1Gateway.arbeidsforhold(
            ident = ident,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed,
            attributter = attributter
        )

        return OppslagResultat(
            meg = megOppslag.meg(
                ident = ident,
                attributter = attributter
            ),
            barn = barnOppslag.barn(
                ident = ident,
                attributter = attributter
            ),
            arbeidsgivereOrganisasjoner = arbeidsgiverOppslag.organisasjoner(
                attributter = attributter,
                arbeidsforhold = arbeidsforhold
            ),
            personligeForetak = personligeForetakOppslag.personligeForetak(
                ident = ident,
                attributter = attributter
            )
        )
    }
}