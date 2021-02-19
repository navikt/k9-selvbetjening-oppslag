package no.nav.k9.inngaende.oppslag

import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.util.*
import no.nav.k9.clients.pdl.generated.HentPersonBolk
import no.nav.k9.utgaende.gateway.PDLProxyGateway
import java.lang.IllegalStateException
import java.time.LocalDate

internal class BarnOppslag(
    private val pdlProxyV1Gateway: PDLProxyGateway
) {

    @KtorExperimentalAPI
    internal suspend fun barn(
        barnasIdenter: List<Ident>,
        attributter: Set<Attributt>
    ) : Set<Barn>? {
        if (!attributter.etterspurtBarn()) return null

        val pdlBarn = pdlProxyV1Gateway.personBolk(barnasIdenter) ?: return null

        return pdlBarn.filter { it.person != null }
            .filter { it.person!!.doedsfall.isNullOrEmpty() }
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

private fun HentPersonBolk.HentPersonBolkResult.tilPdlBarn(): PdlBarn {
    val barn = person!!
    val navn = barn.navn.first()
    val doedsdato = when {
        barn.doedsfall.first().doedsdato.isNullOrBlank() -> null
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
    @JsonProperty(value = "tpsBarn") // TODO: 19/02/2021 Lur måte å migrere dette navnet på?
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
    internal val ident: Ident
)
