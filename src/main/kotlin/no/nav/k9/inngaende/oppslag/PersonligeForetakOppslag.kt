package no.nav.k9.inngaende.oppslag

import no.nav.k9.inngaende.oppslag.PersonligForetakRoller.Companion.fraRolleBeskrivelse
import no.nav.k9.utgaende.gateway.BrregProxyV1Gateway
import no.nav.k9.utgaende.gateway.EnhetsregisterV1Gateway
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class PersonligeForetakOppslag(
    private val enhetsregisterV1Gateway: EnhetsregisterV1Gateway,
    private val brregProxyV1Gateway: BrregProxyV1Gateway
) {
    internal suspend fun personligeForetak(
        ident: Ident,
        attributter: Set<Attributt>
    ) : Set<PersonligForetak<String>>? {
        if (!attributter.etterspurtPersonligForetak()) return null

        return brregProxyV1Gateway.foretak(ident, attributter)!!
            .map {
                it.copy(
                    rollebeskrivelser = it.rollebeskrivelser.filterNot { rollebeskrivelse ->
                        fraRolleBeskrivelse(rollebeskrivelse) == PersonligForetakRoller.IkkePersonligForetakRolle
                    }.toSet()
                )
            }
            .filterNot { it.rollebeskrivelser.isEmpty() }
            .also {
                logger.filtrertForetak("PersonligForetakRoller", it.size)
            }
            .map {
                val enhet = enhetsregisterV1Gateway.enhet(it.organisasjonsnummer, attributter)!!
                PersonligForetak(
                    organisasjonsummer = it.organisasjonsnummer,
                    navn = enhet.navn,
                    registreringsdato = it.registreringsdato,
                    opphørsdato = enhet.opphørsdato,
                    organisasjonsform = PersonligForetakOrganisasjonsform.fraEnhetstype(enhet.enhetstype)
                )
            }
            .filterNot { it.organisasjonsform == PersonligForetakOrganisasjonsform.IkkePersonligForetak }
            .also {
                logger.filtrertForetak("PersonligForetakOrganisasjonsform", it.size)
            }
            .map {
                PersonligForetak(
                    organisasjonsummer = it.organisasjonsummer,
                    navn = it.navn,
                    registreringsdato = it.registreringsdato,
                    opphørsdato = it.opphørsdato,
                    organisasjonsform = it.organisasjonsform.name
                )
            }
            .toSet()
    }

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PersonligeForetakOppslag::class.java)
    }
}

internal fun Logger.filtrertForetak(filtrertPå: String, antallEtterFiltrering: Int) {
    info("Filtrert foretak på $filtrertPå. Antall igjen etter filtrering er $antallEtterFiltrering")
}

internal data class PersonligForetak<T>(
    val organisasjonsummer: String,
    val navn: String?,
    val registreringsdato: LocalDate,
    val opphørsdato: LocalDate?,
    val organisasjonsform: T
)
private enum class PersonligForetakOrganisasjonsform {
    ENK,
    ANS,
    DA,
    IkkePersonligForetak;

    internal companion object {
        internal fun fraEnhetstype(enhetstype: String?) = when (enhetstype?.trim()?.uppercase()) {
            ENK.name -> ENK
            ANS.name -> ANS
            DA.name -> DA
            else -> IkkePersonligForetak
        }
    }
}
private enum class PersonligForetakRoller (internal val rolle: String) {
    Innehaver("Innehaver".uppercase()),
    DeltAnsvar("Deltaker med delt ansvar".uppercase()),
    FulltAnsvar("Deltaker med fullt ansvar".uppercase()),
    IkkePersonligForetakRolle("N/A");

    internal companion object {
        internal fun fraRolleBeskrivelse(rolleBeskrivelse: String?) =
            when (rolleBeskrivelse?.trim()?.uppercase()) {
                Innehaver.rolle -> Innehaver
                DeltAnsvar.rolle -> DeltAnsvar
                FulltAnsvar.rolle -> FulltAnsvar
                else -> IkkePersonligForetakRolle
            }
    }
}

/*
ROLLER:
Bestyrende reder
Daglig leder/ adm.direktør
Deltaker med delt ansvar
Deltaker med fullt ansvar
Forretningsfører
Innehaver
Komplementar
Kontaktperson
Styrets leder
Styremedlem
Nestleder
Observatør
Prokura i fellesskap
Prokura hver for seg
Prokura
Norsk repr. for utenl. enhet
Signatur i fellesskap
Signatur
Signatur hver for seg
Varamedlem
 */
