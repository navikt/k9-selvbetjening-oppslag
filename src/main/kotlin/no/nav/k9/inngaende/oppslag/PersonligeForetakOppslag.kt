package no.nav.k9.inngaende.oppslag

import no.nav.k9.inngaende.oppslag.PersonligForetakRoller.Companion.fraRolle
import no.nav.k9.utgaende.gateway.BrregProxyV1Gateway
import no.nav.k9.utgaende.gateway.EnhetsregisterV1Gateway
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
            .filterNot { fraRolle(it.rolle) == PersonligForetakRoller.IkkePersonligForetakRolle }
            .map {
                val enhet = enhetsregisterV1Gateway.organisasjon(it.organisasjonsnummer, attributter)!!
                PersonligForetak(
                    organisasjonsummer = it.organisasjonsnummer,
                    navn = enhet.navn,
                    registreringsdato = it.registreringsdato,
                    opphørsdato = enhet.opphørsdato,
                    organisasjonsform = PersonligForetakOrganisasjonsform.fraEnhetstype(enhet.enhetstype)
                )
            }
            .filterNot { it.organisasjonsform == PersonligForetakOrganisasjonsform.IkkePersonligForetak }
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
        internal fun fraEnhetstype(enhetstype: String?) = when (enhetstype?.trim()?.toUpperCase()) {
            ENK.name -> ENK
            ANS.name -> ANS
            DA.name -> DA
            else -> IkkePersonligForetak
        }
    }
}
private enum class PersonligForetakRoller (internal val rolle: String) {
    Innehaver("Innehaver".toUpperCase()),
    DeltAnsvar("Deltaker med proratarisk ansvar (delt ansvar)".toUpperCase()),
    FulltAnsvar("Deltaker med solidarisk ansvar (fullt ansvarlig)".toUpperCase()),
    IkkePersonligForetakRolle("N/A");

    internal companion object {
        internal fun fraRolle(rolle: String?) = when (rolle?.trim()?.toUpperCase()) {
            Innehaver.rolle -> Innehaver
            DeltAnsvar.rolle -> DeltAnsvar
            FulltAnsvar.rolle -> FulltAnsvar
            else -> IkkePersonligForetakRolle
        }
    }
}
