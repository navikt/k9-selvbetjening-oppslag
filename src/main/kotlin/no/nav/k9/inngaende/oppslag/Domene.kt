package no.nav.k9.inngaende.oppslag

import no.nav.k9.utgaende.rest.Frilansoppdrag
import no.nav.k9.utgaende.rest.PrivatArbeidsgiver

data class Ident(internal val value: String)

internal enum class Attributt(internal val api: String) {
    aktørId("aktør_id"),
    fornavn("fornavn"),
    mellomnavn("mellomnavn"),
    etternavn("etternavn"),
    fødselsdato("fødselsdato"),

    barnAktørId("barn[].aktør_id"),
    barnIdentitetsnummer("barn[].identitetsnummer"),
    barnFornavn("barn[].fornavn"),
    barnMellomnavn("barn[].mellomnavn"),
    barnEtternavn("barn[].etternavn"),
    barnFødselsdato("barn[].fødselsdato"),
    barnHarSammeAdresse("barn[].har_samme_adresse"),

    arbeidsgivereOrganisasjonerNavn("arbeidsgivere[].organisasjoner[].navn"),
    arbeidsgivereOrganisasjonerOrganisasjonsnummer("arbeidsgivere[].organisasjoner[].organisasjonsnummer"),
    arbeidsgivereOrganisasjonerAnsettelsesperiode("arbeidsgivere[].organisasjoner[].ansettelsesperiode"),

    privateArbeidsgivereAnsettelseperiode("private_arbeidsgivere[].ansettelsesperiode"),
    privateArbeidsgivereOffentligIdent("private_arbeidsgivere[].offentlig_ident"),

    frilansOppdragType("frilans_oppdrag[].type"),
    frilansOppdragAnsettelsesperiode("frilans_oppdrag[].ansettelsesperiode"),

    personligForetakOrganisasjonsnummer("personlige_foretak[].organisasjonsnummer"),
    personligForetakNavn("personlige_foretak[].navn"),
    personligForetakOrganisasjonsform("personlige_foretak[].organisasjonsform"),
    personligForetakRegistreringsdato("personlige_foretak[].registreringsdato"),
    personligForetakOpphørsdato("personlige_foretak[].opphørsdato")
    ;

    internal companion object {
        internal fun fraApi(api: String) : Attributt {
            for (value in values()) {
                if (value.api == api) return value
            }
            throw IllegalStateException("$api er ikke en støttet attributt.")
        }
    }
}
internal fun Set<Attributt>.etterspurtBarn() = any { it.api.startsWith("barn[].") }

internal fun Set<Attributt>.etterspurtArbeidsgivereOrganisasjoner() = any { it.api.startsWith("arbeidsgivere[].organisasjoner[]") }

internal fun Set<Attributt>.etterspurtPrivateArbeidsgivere() = any { it.api.startsWith("private_arbeidsgivere[]") }

internal fun Set<Attributt>.etterspurtPersonligForetak() = any { it.api.startsWith("personlige_foretak[]") }

internal fun Set<Attributt>.etterspurtFrilansOppdrag() = any { it.api.startsWith("frilans_oppdrag[]") }

private val megAttributter = setOf(
    Attributt.aktørId,
    Attributt.fornavn,
    Attributt.mellomnavn,
    Attributt.etternavn,
    Attributt.fødselsdato
)
internal fun Set<Attributt>.etterspurtMeg() = any { it in megAttributter }

internal data class OppslagResultat(
    internal val meg: Meg? = null,
    internal val barn: Set<Barn>? = null,
    internal val arbeidsgivereOrganisasjoner: Set<ArbeidsgiverOrganisasjon>? = null,
    internal val privateArbeidsgivere: Set<PrivatArbeidsgiver>? = null,
    internal val personligeForetak: Set<PersonligForetak<String>>? = null,
    internal val frilansOppdrag: Set<Frilansoppdrag>? = null
)