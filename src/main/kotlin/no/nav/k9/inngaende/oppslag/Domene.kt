package no.nav.k9.inngaende.oppslag

internal data class Ident(internal val value: String)

internal enum class Attributt(internal val api: String) {
    aktoerId("aktoer_id"),
    fornavn("fornavn"),
    mellomnavn("mellomnavn"),
    etternavn("etternavn"),
    foedselsdato("foedselsdato"),

    barnAktoerId("barn[].aktoer_id"),
    barnFornavn("barn[].fornavn"),
    barnMellomnavn("barn[].mellomnavn"),
    barnEtternavn("barn[].etternavn"),
    barnFoedselsdato("barn[].foedselsdato"),

    arbeidsgivereOrganisasjonerNavn("arbeidsgivere[].organisasjoner[].navn"),
    arbeidsgivereOrganisasjonerOrganisasjonsnummer("arbeidsgivere[].organisasjoner[].organisasjonsnummer")

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
internal fun Set<Attributt>.etterspurtBarn() =
    any { it.api.startsWith("barn[].") }
internal fun Set<Attributt>.etterspurtArbeidsgibereOrganaisasjoner() =
    any { it.api.startsWith("arbeidsgivere[].organisasjoner[]") }

private val megAttributter = setOf(
    Attributt.aktoerId,
    Attributt.fornavn,
    Attributt.mellomnavn,
    Attributt.etternavn,
    Attributt.foedselsdato
)
internal fun Set<Attributt>.etterspurtMeg() = any { it in megAttributter }

internal data class OppslagResultat(
    internal val meg: Meg?,
    internal val barn: Set<Barn>?,
    internal val arbeidsgivereOrganisasjoner: Set<ArbeidsgiverOrganisasjon>?
)