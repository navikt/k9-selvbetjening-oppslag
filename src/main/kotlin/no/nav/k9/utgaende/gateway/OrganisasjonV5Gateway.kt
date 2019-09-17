package no.nav.k9.utgaende.gateway

import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.tjeneste.virksomhet.organisasjon.v5.binding.OrganisasjonV5
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.SammensattNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.informasjon.UstrukturertNavn
import no.nav.tjeneste.virksomhet.organisasjon.v5.meldinger.HentNoekkelinfoOrganisasjonRequest

internal class OrganisasjonV5Gateway(
    private val organisasjonV5: OrganisasjonV5
) {

    private companion object {
        private val støttedeAttributter = setOf(
            Attributt.arbeidsgivereOrganisasjonerNavn
        )
    }

    internal fun organisasjon(
        organisasjonsnummer: Organisasjonsnummer,
        attributter: Set<Attributt>
    ) : Organisasjon? {

        if (!attributter.any { it in støttedeAttributter }) return null

        val request = HentNoekkelinfoOrganisasjonRequest().apply {
            orgnummer = organisasjonsnummer.value
        }

        val response = organisasjonV5.hentNoekkelinfoOrganisasjon(request)
        return Organisasjon(
            organisasjonsnummer = Organisasjonsnummer(response.orgnummer),
            navn = response.navn?.stringOrNul()
        )
    }
}

internal data class Organisasjonsnummer(internal val value: String)
internal data class Organisasjon(internal val organisasjonsnummer: Organisasjonsnummer, internal val navn: String?)
private fun SammensattNavn.stringOrNul() : String? {
    val medNavn=  (this as UstrukturertNavn).navnelinje.filterNot {  it.isNullOrBlank() }
    return if (medNavn.isNullOrEmpty()) null
    else medNavn.joinToString(", ")
}