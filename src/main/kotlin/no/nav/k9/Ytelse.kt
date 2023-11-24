package no.nav.k9

import io.ktor.server.application.ApplicationCall
import no.nav.k9.utgaende.rest.NavHeaders.XK9Ytelse
import no.nav.siftilgangskontroll.core.behandling.Behandling

enum class Ytelse {
    OMSORGSPENGER_UTVIDET_RETT,
    OMSORGSPENGER_MIDLERTIDIG_ALENE,
    ETTERSENDING,
    OMSORGSDAGER_ALENEOMSORG,
    OMSORGSPENGER_UTBETALING_ARBEIDSTAKER,
    OMSORGSPENGER_UTBETALING_SNF,
    PLEIEPENGER_LIVETS_SLUTTFASE,
    ETTERSENDING_PLEIEPENGER_SYKT_BARN,
    ETTERSENDING_PLEIEPENGER_LIVETS_SLUTTFASE,
    ETTERSENDING_OMP,
    PLEIEPENGER_SYKT_BARN,
    DINE_PLEIEPENGER,
    ENDRINGSMELDING_PLEIEPENGER_SYKT_BARN;

    fun somBehandling(): Behandling = when (this) {
        PLEIEPENGER_SYKT_BARN, ENDRINGSMELDING_PLEIEPENGER_SYKT_BARN,  ETTERSENDING_PLEIEPENGER_SYKT_BARN, DINE_PLEIEPENGER -> Behandling.PLEIEPENGER_SYKT_BARN
        PLEIEPENGER_LIVETS_SLUTTFASE, ETTERSENDING_PLEIEPENGER_LIVETS_SLUTTFASE -> Behandling.PLEIEPENGER_I_LIVETS_SLUTTFASE
        OMSORGSPENGER_UTVIDET_RETT, OMSORGSPENGER_MIDLERTIDIG_ALENE, OMSORGSDAGER_ALENEOMSORG, ETTERSENDING_OMP -> Behandling.OMSORGSPENGER_RAMMEMELDING
        OMSORGSPENGER_UTBETALING_ARBEIDSTAKER, OMSORGSPENGER_UTBETALING_SNF -> Behandling.OMSORGSPENGERUTBETALING
        ETTERSENDING -> Behandling.PLEIEPENGER_SYKT_BARN
    }
}


fun ApplicationCall.ytelseFraHeader(): Ytelse {
    val ytelseFraHeader = request.headers[XK9Ytelse] ?: throw IllegalArgumentException("Mangler header $XK9Ytelse")
    return Ytelse.valueOf(ytelseFraHeader)
}
