package no.nav.k9.inngaende.oppslag

import io.ktor.util.*
import no.nav.k9.clients.pdl.generated.HentPerson
import no.nav.k9.utgaende.gateway.AktoerRegisterV1Gateway
import no.nav.k9.utgaende.gateway.PDLProxyGateway
import no.nav.k9.utgaende.gateway.TpsProxyV1Gateway
import no.nav.k9.utgaende.rest.AktørId
import no.nav.k9.utgaende.rest.TpsPerson
import java.time.LocalDate

internal class MegOppslag(
    private val aktoerRegisterV1Gateway: AktoerRegisterV1Gateway,
    private val tpsProxyV1Gateway: TpsProxyV1Gateway,
    private val pdlProxyGateway: PDLProxyGateway,
) {

    @KtorExperimentalAPI
    internal suspend fun meg(
        ident: Ident,
        attributter: Set<Attributt>,
    ): Meg {
        val pdlPerson = pdlProxyGateway.person(
            ident = ident,
            attributter = attributter
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
            )?.first()?.ident?.let { AktørId(it) },

            pdlPerson = pdlPerson?.tilPdlPerson()
        )
    }

    private fun HentPerson.Person.tilPdlPerson(): PdlPerson {
        val navn1 = this.navn[0]
        return PdlPerson(
            fornavn = navn1.fornavn,
            mellomnavn = navn1.mellomnavn,
            etternavn = navn1.etternavn,
            forkortetNavn = navn1.forkortetNavn,
            fødselsdato = LocalDate.parse(foedsel.first().foedselsdato)
        )
    }
}

data class PdlPerson(
    internal val fornavn: String,
    internal val mellomnavn: String?,
    internal val etternavn: String,
    internal val forkortetNavn: String?,
    internal val fødselsdato: LocalDate,
)

internal data class Meg(
    internal val tpsPerson: TpsPerson?,
    internal val pdlPerson: PdlPerson?,
    internal val aktørId: AktørId?,
)
