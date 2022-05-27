package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.gateway.PDLProxyGateway
import no.nav.siftilgangskontroll.pdl.generated.enums.ForelderBarnRelasjonRolle
import no.nav.siftilgangskontroll.pdl.generated.hentperson.Person
import java.lang.IllegalStateException
import java.time.LocalDate

internal class MegOppslag(
    private val pdlProxyGateway: PDLProxyGateway,
) {

    internal suspend fun meg(
        ident: Ident,
        attributter: Set<Attributt>,
    ): Meg {
        val pdlPerson = pdlProxyGateway.person()

        val aktørId = pdlProxyGateway.aktørId(
            ident = ident,
            attributter = attributter
        )

        return Meg(
            aktørId = aktørId?.let { Ident(it.value) },
            pdlPerson = pdlPerson.tilPdlPerson()
        )
    }

    private fun Person.tilPdlPerson(): PdlPerson {
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
    .map { Ident(it.relatertPersonsIdent!!) }

data class PdlPerson(
    internal val fornavn: String,
    internal val mellomnavn: String?,
    internal val etternavn: String,
    internal val forkortetNavn: String?,
    internal val fødselsdato: LocalDate,
    internal val barnIdenter: List<Ident>
)

internal data class Meg(
    internal val pdlPerson: PdlPerson?,
    internal val aktørId: Ident?
)
