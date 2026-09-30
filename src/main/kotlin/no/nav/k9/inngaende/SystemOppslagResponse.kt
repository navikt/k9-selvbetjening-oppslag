package no.nav.k9.inngaende

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.inngaende.oppslag.SystemoppslagBarn
import no.nav.k9.inngaende.oppslag.SystemoppslagPdlBarn
import no.nav.siftilgangskontroll.pdl.generated.enums.IdentGruppe
import no.nav.siftilgangskontroll.pdl.generated.hentbarn.Adressebeskyttelse
import no.nav.siftilgangskontroll.pdl.generated.hentidenterbolk.HentIdenterBolkResult
import java.time.LocalDate

/**
 * Kontrakten mot konsumentene av systemendepunktene under /system. Verdier som er null skal ikke være med i svaret.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class IdenterBolkResponse(
    val ident: String,
    val identer: List<IdentInformasjonResponse>?,
    val code: String,
)

internal data class IdentInformasjonResponse(
    val ident: String,
    val gruppe: IdentGruppe,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class SystemBarnResponse(
    val aktørId: IdentResponse?,
    val pdlBarn: SystemPdlBarnResponse?,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class SystemPdlBarnResponse(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val fødselsdato: LocalDate,
    val dødsdato: LocalDate?,
    val ident: IdentResponse,
    val adressebeskyttelse: List<Adressebeskyttelse>,
)

internal data class IdentResponse(val value: String)

internal fun HentIdenterBolkResult.somResponse() = IdenterBolkResponse(
    ident = ident,
    identer = identer?.map { IdentInformasjonResponse(ident = it.ident, gruppe = it.gruppe) },
    code = code,
)

internal fun SystemoppslagBarn.somResponse() = SystemBarnResponse(
    aktørId = aktørId?.somResponse(),
    pdlBarn = pdlBarn?.somResponse(),
)

private fun SystemoppslagPdlBarn.somResponse() = SystemPdlBarnResponse(
    fornavn = fornavn,
    mellomnavn = mellomnavn,
    etternavn = etternavn,
    fødselsdato = fødselsdato,
    dødsdato = dødsdato,
    ident = ident.somResponse(),
    adressebeskyttelse = adressebeskyttelse,
)

private fun Ident.somResponse() = IdentResponse(value)
