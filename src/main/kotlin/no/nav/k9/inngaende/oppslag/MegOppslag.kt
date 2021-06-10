package no.nav.k9.inngaende.oppslag

import io.ktor.util.*
import no.nav.k9.clients.pdl.generated.HentIdent
import no.nav.k9.clients.pdl.generated.HentPerson
import no.nav.k9.utgaende.gateway.PDLProxyGateway
import no.nav.k9.utgaende.gateway.TpsProxyV1Gateway
import no.nav.k9.utgaende.rest.TpsPerson
import java.lang.IllegalStateException
import java.time.LocalDate

internal class MegOppslag(
    private val tpsProxyV1Gateway: TpsProxyV1Gateway,
    private val pdlProxyGateway: PDLProxyGateway,
) {

    @KtorExperimentalAPI
    internal suspend fun meg(
        ident: Ident,
        attributter: Set<Attributt>,
    ): Meg {
        val pdlPerson = pdlProxyGateway.person(
            ident = ident
        )

        val tpsPerson = tpsProxyV1Gateway.person(
            ident = ident,
            attributter = attributter
        )
        return Meg(
            tpsPerson = tpsPerson,
            aktørId = pdlProxyGateway.aktørId(
                ident = ident,
                attributter = attributter
            )?.tilAktørId(),
            pdlPerson = pdlPerson?.tilPdlPerson()
        )
    }

    private fun HentPerson.Person.tilPdlPerson(): PdlPerson {
        println("Navn på person: $navn")
        val navn = this.navn.firstOrNull()?: throw IllegalStateException("Det må eksistere navn på person.")
        return PdlPerson(
            fornavn = navn.fornavn,
            mellomnavn = navn.mellomnavn,
            etternavn = navn.etternavn,
            forkortetNavn = navn.forkortetNavn,
            fødselsdato = LocalDate.parse(foedsel.first().foedselsdato),
            barnIdenter = this.barnIdenter()
        )
    }
}

fun HentPerson.Person.barnIdenter(): List<Ident> = forelderBarnRelasjon
    .filter { it.relatertPersonsRolle == HentPerson.Familierelasjonsrolle.BARN }
    .map { Ident(it.relatertPersonsIdent) }

fun List<HentIdent.IdentInformasjon>.tilAktørId(): Ident = Ident(first().ident)

data class PdlPerson(
    internal val fornavn: String,
    internal val mellomnavn: String?,
    internal val etternavn: String,
    internal val forkortetNavn: String?,
    internal val fødselsdato: LocalDate,
    internal val barnIdenter: List<Ident>
)

internal data class Meg(
    internal val tpsPerson: TpsPerson?,
    internal val pdlPerson: PdlPerson?,
    internal val aktørId: Ident?,
)
