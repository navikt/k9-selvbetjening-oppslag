package no.nav.k9.utgaende.gateway

import no.nav.k9.clients.pdl.generated.hentbarn.HentPersonBolkResult
import no.nav.k9.clients.pdl.generated.hentperson.Person
import no.nav.k9.clients.pdl.generated.hentident.IdentInformasjon
import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.inngaende.oppslag.OppslagService.Companion.støttedeAttributter
import no.nav.k9.utgaende.rest.PDLProxy

class PDLProxyGateway(
    val pdlProxy: PDLProxy,
) {

    internal suspend fun person(
        ident: Ident
    ): Person = pdlProxy.person(ident.value)

    internal suspend fun barn(
        identer: List<Ident>,
    ): List<HentPersonBolkResult> = pdlProxy.barn(identer.map { it.value })

    internal suspend fun aktørId(
        ident: Ident,
        attributter: Set<Attributt>,
    ): List<IdentInformasjon>? = when {
        attributter.any { it in støttedeAttributter } -> pdlProxy.aktørId(ident.value)
        else -> null
    }
}

