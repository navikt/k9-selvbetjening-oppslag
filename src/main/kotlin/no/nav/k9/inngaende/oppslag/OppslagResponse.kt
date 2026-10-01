package no.nav.k9.inngaende.oppslag

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.k9.integrasjon.aareg.TypeArbeidssted
import java.time.LocalDate

/**
 * Kontrakten mot konsumentene av /meg og /arbeidsgivere.
 * Felt som ikke er etterspurt eller som mangler verdi (null) skal ikke være med i svaret.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class OppslagResponse(
    @get:JsonProperty("aktør_id") val aktørId: String? = null,
    val fornavn: String? = null,
    val mellomnavn: String? = null,
    val etternavn: String? = null,
    val fødselsdato: LocalDate? = null,
    val barn: List<BarnResponse>? = null,
    val arbeidsgivere: ArbeidsgivereResponse? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class BarnResponse(
    @get:JsonProperty("aktør_id") val aktørId: String? = null,
    val identitetsnummer: String? = null,
    val fornavn: String? = null,
    val mellomnavn: String? = null,
    val etternavn: String? = null,
    val fødselsdato: LocalDate? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class ArbeidsgivereResponse(
    val organisasjoner: List<OrganisasjonResponse>? = null,
    @get:JsonProperty("private_arbeidsgivere") val privateArbeidsgivere: List<PrivatArbeidsgiverResponse>? = null,
    val frilansoppdrag: List<FrilansoppdragResponse>? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class OrganisasjonResponse(
    val organisasjonsnummer: String? = null,
    val navn: String? = null,
    @get:JsonProperty("ansatt_fom") val ansattFom: LocalDate? = null,
    @get:JsonProperty("ansatt_tom") val ansattTom: LocalDate? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class PrivatArbeidsgiverResponse(
    @get:JsonProperty("offentlig_ident") val offentligIdent: String? = null,
    @get:JsonProperty("ansatt_fom") val ansattFom: LocalDate? = null,
    @get:JsonProperty("ansatt_tom") val ansattTom: LocalDate? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
internal data class FrilansoppdragResponse(
    val type: TypeArbeidssted,
    @get:JsonProperty("ansatt_fom") val ansattFom: LocalDate,
    @get:JsonProperty("ansatt_tom") val ansattTom: LocalDate? = null,
    @get:JsonProperty("offentlig_ident") val offentligIdent: String? = null,
    val organisasjonsnummer: String? = null,
    val navn: String? = null,
)

internal fun OppslagResultat.somResponse(attributter: Set<Attributt>): OppslagResponse {
    fun <T> hvisEtterspurt(attributt: Attributt, verdi: () -> T): T? =
        if (attributter.contains(attributt)) verdi() else null

    val barn = if (attributter.etterspurtBarn()) {
        barn.orEmpty().map {
            BarnResponse(
                aktørId = hvisEtterspurt(Attributt.barnAktørId) { it.aktørId!!.value },
                identitetsnummer = hvisEtterspurt(Attributt.barnIdentitetsnummer) { it.pdlBarn!!.ident.value },
                fornavn = hvisEtterspurt(Attributt.barnFornavn) { it.pdlBarn!!.fornavn },
                mellomnavn = hvisEtterspurt(Attributt.barnMellomnavn) { it.pdlBarn!!.mellomnavn },
                etternavn = hvisEtterspurt(Attributt.barnEtternavn) { it.pdlBarn!!.etternavn },
                fødselsdato = hvisEtterspurt(Attributt.barnFødselsdato) { it.pdlBarn!!.fødselsdato },
            )
        }
    } else null

    val organisasjoner = if (attributter.etterspurtArbeidsgivereOrganisasjoner()) {
        arbeidsgivereOrganisasjoner.orEmpty().map {
            val ansettelsesperiode = attributter.contains(Attributt.arbeidsgivereOrganisasjonerAnsettelsesperiode)
            OrganisasjonResponse(
                organisasjonsnummer = hvisEtterspurt(Attributt.arbeidsgivereOrganisasjonerOrganisasjonsnummer) { it.organisasjonsnummer },
                navn = hvisEtterspurt(Attributt.arbeidsgivereOrganisasjonerNavn) { it.navn },
                ansattFom = if (ansettelsesperiode) it.ansattFom else null,
                ansattTom = if (ansettelsesperiode) it.ansattTom else null,
            )
        }
    } else null

    val privateArbeidsgivere = if (attributter.etterspurtPrivateArbeidsgivere()) {
        privateArbeidsgivere.orEmpty().map {
            val ansettelsesperiode = attributter.contains(Attributt.privateArbeidsgivereAnsettelseperiode)
            PrivatArbeidsgiverResponse(
                offentligIdent = hvisEtterspurt(Attributt.privateArbeidsgivereOffentligIdent) { it.offentligIdent },
                ansattFom = if (ansettelsesperiode) it.ansattFom else null,
                ansattTom = if (ansettelsesperiode) it.ansattTom else null,
            )
        }
    } else null

    val frilansoppdrag = if (attributter.etterspurtFrilansoppdrag()) {
        frilansoppdrag.orEmpty().map {
            FrilansoppdragResponse(
                type = it.type,
                ansattFom = it.ansattFom,
                ansattTom = it.ansattTom,
                offentligIdent = it.offentligIdent,
                organisasjonsnummer = it.organisasjonsnummer,
                navn = it.navn,
            )
        }
    } else null

    val arbeidsgivere = if (organisasjoner != null || privateArbeidsgivere != null || frilansoppdrag != null) {
        ArbeidsgivereResponse(organisasjoner, privateArbeidsgivere, frilansoppdrag)
    } else null

    val megEtterspurt = attributter.etterspurtMeg()
    return OppslagResponse(
        aktørId = if (megEtterspurt) hvisEtterspurt(Attributt.aktørId) { meg!!.aktørId!!.value } else null,
        fornavn = hvisEtterspurt(Attributt.fornavn) { meg!!.pdlPerson!!.fornavn },
        mellomnavn = hvisEtterspurt(Attributt.mellomnavn) { meg!!.pdlPerson!!.mellomnavn },
        etternavn = hvisEtterspurt(Attributt.etternavn) { meg!!.pdlPerson!!.etternavn },
        fødselsdato = hvisEtterspurt(Attributt.fødselsdato) { meg!!.pdlPerson!!.fødselsdato },
        barn = barn,
        arbeidsgivere = arbeidsgivere,
    )
}
