package no.nav.k9.inngaende.oppslag

import no.nav.k9.clients.pdl.generated.enums.ForelderBarnRelasjonRolle
import no.nav.k9.clients.pdl.generated.hentident.IdentInformasjon
import no.nav.k9.clients.pdl.generated.hentperson.Person
import no.nav.k9.utgaende.gateway.PDLProxyGateway
import no.nav.k9.utgaende.gateway.TpsProxyV1Gateway
import no.nav.k9.utgaende.rest.TpsPerson
import java.lang.IllegalStateException
import java.time.LocalDate

internal class MegOppslag(
    private val tpsProxyV1Gateway: TpsProxyV1Gateway,
    private val pdlProxyGateway: PDLProxyGateway,
) {

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
            pdlPerson = pdlPerson.tilPdlPerson()
        )
    }

    private fun Person.tilPdlPerson(): PdlPerson {
        println("Navn på person: $navn") // TODO: 10/06/2021 fjern før prodsetting.
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

fun Person.barnIdenter(): List<Ident> = forelderBarnRelasjon
    .filter { it.relatertPersonsRolle == ForelderBarnRelasjonRolle.BARN }
    .map { Ident(it.relatertPersonsIdent) }

fun List<IdentInformasjon>.tilAktørId(): Ident = Ident(first().ident)

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
