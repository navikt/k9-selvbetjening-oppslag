package no.nav.k9.inngaende.oppslag

import no.nav.k9.Ytelse
import no.nav.k9.utgaende.gateway.PDLProxyGateway
import no.nav.siftilgangskontroll.pdl.generated.hentbarn.Person
import java.time.LocalDate

internal class BarnOppslag(
    private val pdlProxyV1Gateway: PDLProxyGateway,
) {

    internal suspend fun barn(
        barnasIdenter: List<Ident>,
        attributter: Set<Attributt>,
        ytelse: Ytelse,
    ): Set<Barn>? {
        if (!attributter.etterspurtBarn()) return null

        return when {
            barnasIdenter.isEmpty() -> null
            else -> pdlProxyV1Gateway.barn(barnasIdenter, ytelse)
                .map { barn ->
                    val pdlBarn = barn.tilPdlBarn()
                    val aktørId = pdlProxyV1Gateway.aktørId(
                        ident = Ident(pdlBarn.ident.value),
                        attributter = attributter
                    )
                    Barn(
                        pdlBarn = pdlBarn,
                        aktørId = aktørId?.let { Ident(it.value) }
                    )
                }.toSet()
        }
    }
}

private fun Person.tilPdlBarn(): PdlBarn {
    val barn = this
    val navn = barn.navn.first()
    val doedsdato = when {
        barn.doedsfall.isEmpty() -> null
        else -> LocalDate.parse(barn.doedsfall.first().doedsdato!!)
    }
    val foedselsdato = when {
        barn.foedselsdato.first().foedselsdato.isNullOrBlank() -> throw IllegalStateException("Barnets fødselsnummer var tom eller null.")
        else -> LocalDate.parse(barn.foedselsdato.first().foedselsdato!!)
    }
    val ident = barn.folkeregisteridentifikator.first().identifikasjonsnummer
    return PdlBarn(
        fornavn = navn.fornavn,
        mellomnavn = navn.mellomnavn,
        etternavn = navn.etternavn,
        fødselsdato = foedselsdato,
        dødsdato = doedsdato,
        ident = Ident(ident)
    )
}

data class Barn(
    val pdlBarn: PdlBarn?,
    val aktørId: Ident?,
)

data class PdlBarn(
   val fornavn: String,
   val mellomnavn: String?,
   val etternavn: String,
   val fødselsdato: LocalDate,
   val dødsdato: LocalDate?,
   val ident: Ident,
)
