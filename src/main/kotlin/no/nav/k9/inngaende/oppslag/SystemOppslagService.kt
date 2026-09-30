package no.nav.k9.inngaende.oppslag

import no.nav.k9.Ytelse
import no.nav.k9.integrasjon.pdl.PdlProxyService
import no.nav.siftilgangskontroll.core.tilgang.BarnResponse
import no.nav.siftilgangskontroll.pdl.generated.enums.IdentGruppe
import no.nav.siftilgangskontroll.pdl.generated.hentbarn.Adressebeskyttelse
import no.nav.siftilgangskontroll.pdl.generated.hentbarn.Person
import no.nav.siftilgangskontroll.pdl.generated.hentidenterbolk.HentIdenterBolkResult
import org.slf4j.LoggerFactory
import java.time.LocalDate

class SystemOppslagService(
    private val pdlProxyService: PdlProxyService,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(SystemOppslagService::class.java)
    }

    fun hentIdenter(identer: List<String>, identGrupper: List<IdentGruppe>): List<HentIdenterBolkResult> {
        logger.info("Henter identer med systemkall.")
        return pdlProxyService.hentIdenter(identer, identGrupper)
    }

    fun hentBarn(identer: List<String>, ytelse: Ytelse): List<SystemoppslagBarn> {
        logger.info("Henter barn med systemkall.")
        return pdlProxyService.hentBarn(identer, ytelse).map { br: BarnResponse ->
            val aktørId = pdlProxyService.aktørId(
                ident = Ident(br.ident),
                attributter = setOf(Attributt.barnAktørId),
                system = true
            )
            SystemoppslagBarn(
                pdlBarn = br.barn.tilPdlBarn(),
                aktørId = aktørId?.let { Ident(it.value) }
            )
        }
    }

    fun Person.tilPdlBarn(): SystemoppslagPdlBarn {
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

        val adressebeskyttelse = barn.adressebeskyttelse

        return SystemoppslagPdlBarn(
            fornavn = navn.fornavn,
            mellomnavn = navn.mellomnavn,
            etternavn = navn.etternavn,
            fødselsdato = foedselsdato,
            dødsdato = doedsdato,
            ident = Ident(ident),
            adressebeskyttelse = adressebeskyttelse
        )
    }
}

data class SystemoppslagPdlBarn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fødselsdato: LocalDate,
    val dødsdato: LocalDate?,
    val ident: Ident,
    val adressebeskyttelse: List<Adressebeskyttelse>,
)

data class SystemoppslagBarn(
    val pdlBarn: SystemoppslagPdlBarn?,
    val aktørId: Ident?,
)
