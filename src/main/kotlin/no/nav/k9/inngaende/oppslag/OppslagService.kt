package no.nav.k9.inngaende.oppslag

import io.ktor.util.*
import no.nav.k9.utgaende.gateway.*
import no.nav.k9.utgaende.gateway.ArbeidsgiverOgArbeidstakerRegisterV1Gateway
import java.time.LocalDate


internal class OppslagService(
    private val arbeidsgiverOgArbeidstakerRegisterV1Gateway: ArbeidsgiverOgArbeidstakerRegisterV1Gateway,
    enhetsregisterV1Gateway: EnhetsregisterV1Gateway,
    tpsProxyV1Gateway: TpsProxyV1Gateway,
    brregProxyV1Gateway: BrregProxyV1Gateway,
    pdlProxyGateway: PDLProxyGateway,
) {
    internal companion object {
        val støttedeAttributter = setOf(
            Attributt.aktørId,
            Attributt.barnAktørId
        )

        val personAttributter = setOf(
            Attributt.fornavn,
            Attributt.mellomnavn,
            Attributt.etternavn,
            Attributt.fødselsdato,
            Attributt.kontonummer
        )

        val barnAttributter = setOf(
            Attributt.barnAktørId, // Må hente opp barn for å vite hvem vi skal slå opp aktørId på
            Attributt.barnIdentitetsnummer,
            Attributt.barnFornavn,
            Attributt.barnMellomnavn,
            Attributt.barnEtternavn,
            Attributt.barnHarSammeAdresse,
            Attributt.barnFødselsdato
        )
    }

    private val megOppslag = MegOppslag(
        tpsProxyV1Gateway = tpsProxyV1Gateway,
        pdlProxyGateway = pdlProxyGateway
    )

    private val barnOppslag = BarnOppslag(
        pdlProxyV1Gateway = pdlProxyGateway
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
        tilOgMed: LocalDate,
    ): OppslagResultat {

        val arbeidsforhold = arbeidsgiverOgArbeidstakerRegisterV1Gateway.arbeidsforhold(
            ident = ident,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed,
            attributter = attributter
        )

        val meg = megOppslag.meg(
            ident = ident,
            attributter = attributter
        )

        return OppslagResultat(
            meg = meg,
            barn = barnOppslag.barn(
                barnasIdenter = meg.pdlPerson?.barnIdenter ?: listOf(),
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
