package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.*
import no.nav.k9.utgaende.rest.Arbeidsgivere
import no.nav.k9.utgaende.rest.OrganisasjonArbeidsgivere
import org.slf4j.LoggerFactory
import java.time.LocalDate


internal class OppslagService(
    private val arbeidsgiverOgArbeidstakerRegisterGateway: ArbeidsgiverOgArbeidstakerRegisterGateway,
    private val arbeidsgiverOppslag: ArbeidsgivereOppslag,
    private val megOppslag: MegOppslag,
    private val barnOppslag: BarnOppslag
) {

    private val logger = LoggerFactory.getLogger(OppslagService::class.java)

    internal companion object {
        val støttedeAttributter = setOf(
            Attributt.aktørId,
            Attributt.barnAktørId
        )

        val personAttributter = setOf(
            Attributt.fornavn,
            Attributt.mellomnavn,
            Attributt.etternavn,
            Attributt.fødselsdato
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

    internal suspend fun oppslag(
        ident: Ident,
        attributter: Set<Attributt>,
        fraOgMed: LocalDate,
        tilOgMed: LocalDate,
    ): OppslagResultat {

        val arbeidsgivere = arbeidsgiverOgArbeidstakerRegisterGateway.arbeidsgivere(ident, fraOgMed, tilOgMed, attributter)

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
                arbeidsgivere = arbeidsgivere
            ),
            privateArbeidsgivere = arbeidsgivere?.privateArbeidsgivere,
            frilansoppdrag = arbeidsgivere?.frilansoppdrag?.map {
                it.copy(
                    navn = if (it.organisasjonsnummer != null) arbeidsgiverOppslag.hentNavn(
                        it.organisasjonsnummer, setOf(Attributt.arbeidsgivereOrganisasjonerNavn)
                    ) else null
                )
            }?.toSet()
        )
    }

    internal suspend fun arbeidsgivere(
        attributter: Set<Attributt>,
        organisasjoner: Set<String>,
    ): OppslagResultat {

        val arbeidsgivere = Arbeidsgivere(
            organisasjoner = organisasjoner
                .map { OrganisasjonArbeidsgivere(it) }
                .toSet()
        )

        return OppslagResultat(
            arbeidsgivereOrganisasjoner = arbeidsgiverOppslag.organisasjoner(
                attributter = attributter,
                arbeidsgivere = arbeidsgivere
            )
        )
    }
}
