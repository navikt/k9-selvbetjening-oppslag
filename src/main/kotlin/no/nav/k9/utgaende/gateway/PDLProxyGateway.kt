package no.nav.k9.utgaende.gateway

import io.ktor.util.*
import no.nav.k9.clients.pdl.generated.HentBarn
import no.nav.k9.clients.pdl.generated.HentIdent
import no.nav.k9.clients.pdl.generated.HentPerson
import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.inngaende.oppslag.OppslagService.Companion.støttedeAttributter
import no.nav.k9.utgaende.rest.PDLProxy

class PDLProxyGateway(
    val pdlProxy: PDLProxy,
) {

    @KtorExperimentalAPI
    internal suspend fun person(
        ident: Ident
    ): HentPerson.Person = pdlProxy.person(ident.value)

    @KtorExperimentalAPI
    internal suspend fun barn(
        identer: List<Ident>,
    ): List<HentBarn.HentPersonBolkResult> = pdlProxy.barn(identer.map { it.value })

    @KtorExperimentalAPI
    internal suspend fun aktørId(
        ident: Ident,
        attributter: Set<Attributt>,
    ): List<HentIdent.IdentInformasjon>? = when {
        attributter.any { it in støttedeAttributter } -> pdlProxy.aktørId(ident.value)
        else -> null
    }
}
