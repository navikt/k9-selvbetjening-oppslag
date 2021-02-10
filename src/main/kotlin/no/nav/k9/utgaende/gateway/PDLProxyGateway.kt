package no.nav.k9.utgaende.gateway

import io.ktor.util.*
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.utgaende.rest.PDLProxy
import no.nav.k9.utgaende.rest.PersonPdlResponse

class PDLProxyGateway(
    val pdlProxy: PDLProxy
) {

    @KtorExperimentalAPI
    internal suspend fun person(
        ident: Ident
    ): PersonPdlResponse {
        return pdlProxy.person(ident.value)
    }
}
