package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.*
import no.nav.k9.utgaende.rest.Arbeidsgivere
import no.nav.k9.utgaende.rest.OrganisasjonArbeidsgivere
import no.nav.k9.utgaende.rest.aaregv2.ArbeidsgiverOgArbeidstakerRegisterV2
import org.slf4j.LoggerFactory
import java.time.LocalDate


internal class OppslagService(
    private val arbeidsgiverOgArbeidstakerRegisterV1Gateway: ArbeidsgiverOgArbeidstakerRegisterV1Gateway,
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
        val arbeidsgivere = arbeidsgiverOgArbeidstakerRegisterV1Gateway.arbeidsgivere(
            ident = ident,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed,
            attributter = attributter
        )
        val arbeidsgivereFraV2 = arbeidsgiverOgArbeidstakerRegisterV1Gateway.arbeidsgivereV2(ident, fraOgMed, tilOgMed, attributter)

        arbeidsgivere?.also {
            val arbeidsgivereFraV1erLikV2 = arbeidsgivere == arbeidsgivereFraV2
            logger.info("Migreringsjekk til aareg v2. Er like=$arbeidsgivereFraV1erLikV2")
            if(!arbeidsgivereFraV1erLikV2){
                logger.info("Antall organisasjoner: V1=${arbeidsgivere.organisasjoner.size}, V2=${arbeidsgivereFraV2?.organisasjoner?.size}")
                logger.info("Antall frilansoppdrag: V1=${arbeidsgivere.frilansoppdrag.size}, V2=${arbeidsgivereFraV2?.frilansoppdrag?.size}")
                logger.info("Antall private arbeidsgivere: V1=${arbeidsgivere.privateArbeidsgivere.size}, V2=${arbeidsgivereFraV2?.privateArbeidsgivere?.size}")

                if(arbeidsgivereFraV2?.organisasjoner != null && arbeidsgivere.organisasjoner.size == arbeidsgivereFraV2.organisasjoner.size) {
                    val v1 = arbeidsgivere.organisasjoner.toList().sortedBy { it.organisasjonsnummer }
                    val v2 = arbeidsgivereFraV2.organisasjoner.toList().sortedBy { it.organisasjonsnummer }
                    for(i in v1.indices){
                        if(v1[i].organisasjonsnummer != v2[i].organisasjonsnummer) logger.info("Forskjell på organisasjonsnummer")
                        if(v1[i].ansattFom != v2[i].ansattFom) logger.info("Forskjell på ansattFom. V1 = ${v1[i].ansattFom}, V2 = ${v2[i].ansattFom}")
                        if(v1[i].ansattTom != v2[i].ansattTom) logger.info("Forskjell på ansattTom")
                    }
                }
            }
        }

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
