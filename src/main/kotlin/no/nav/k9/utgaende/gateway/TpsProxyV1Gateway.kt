package no.nav.k9.utgaende.gateway

import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.inngaende.oppslag.OppslagService.Companion.barnAttributter
import no.nav.k9.inngaende.oppslag.OppslagService.Companion.personAttributter
import no.nav.k9.utgaende.rest.TpsBarn
import no.nav.k9.utgaende.rest.TpsPerson
import no.nav.k9.utgaende.rest.TpsProxyV1

internal class TpsProxyV1Gateway(
    private val tpsProxyV1: TpsProxyV1
) {

    internal suspend fun barn(
        ident: Ident,
        attributter: Set<Attributt>
    ): Set<TpsBarn>? {
        return if (!attributter.any { it in barnAttributter }) null
        else tpsProxyV1.barn(ident)
    }

    internal suspend fun person(
        ident: Ident,
        attributter: Set<Attributt>
    ) : TpsPerson? {
        return if (!attributter.any { it in personAttributter }) null
        else tpsProxyV1.person(ident)
    }
}
