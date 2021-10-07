package no.nav.k9.inngaende.oppslag

import no.nav.k9.clients.pdl.generated.hentbarn.HentPersonBolkResult
import no.nav.k9.utgaende.gateway.PDLProxyGateway
import java.time.LocalDate

internal class BarnOppslag(
    private val pdlProxyV1Gateway: PDLProxyGateway,
) {


    internal suspend fun barn(
        barnasIdenter: List<Ident>,
        attributter: Set<Attributt>,
    ): Set<Barn>? {
        if (!attributter.etterspurtBarn()) return null

        return when {
            barnasIdenter.isEmpty() -> null
            else -> pdlProxyV1Gateway.barn(barnasIdenter)
                .map {
                    Barn(
                        pdlBarn = it.tilPdlBarn(),
                        aktørId = pdlProxyV1Gateway.aktørId(
                            ident = Ident(it.ident),
                            attributter = attributter
                        )?.tilAktørId()
                    )
                }.toSet()
        }
    }
}

private fun HentPersonBolkResult.tilPdlBarn(): PdlBarn {
    val barn = person!!
    val navn = barn.navn.first()
    val doedsdato = when {
        barn.doedsfall.isEmpty() -> null
        else -> LocalDate.parse(barn.doedsfall.first().doedsdato!!)
    }
    val foedselsdato = when {
        barn.foedsel.first().foedselsdato.isNullOrBlank() -> throw IllegalStateException("Barnets fødselsnummer var tom eller null.")
        else -> LocalDate.parse(barn.foedsel.first().foedselsdato!!)
    }
    return PdlBarn(
        fornavn = navn.fornavn,
        mellomnavn = navn.mellomnavn,
        etternavn = navn.etternavn,
        forkortetNavn = navn.forkortetNavn,
        fødselsdato = foedselsdato,
        dødsdato = doedsdato,
        ident = Ident(ident)
    )
}

internal data class Barn(
    internal val pdlBarn: PdlBarn?,
    internal val aktørId: Ident?,
)

data class PdlBarn(
    internal val fornavn: String,
    internal val mellomnavn: String?,
    internal val etternavn: String,
    internal val forkortetNavn: String?,
    internal val fødselsdato: LocalDate,
    internal val dødsdato: LocalDate?,
    internal val ident: Ident,
)
