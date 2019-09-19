package no.nav.k9.inngaende.oppslag

import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person
import no.nav.k9.utgaende.rest.AktørId

internal data class Ident(internal val value: String)

internal enum class Attributt(internal val api: String?) {
    aktørId("aktør_id"),
    fornavn("fornavn"),
    mellomnavn("mellomnavn"),
    etternavn("etternavn"),
    fødselsdato("fødselsdato"),
    status(null),
    diskresjonskode(null),

    barnAktørId("barn[].aktør_id"),
    barnFornavn("barn[].fornavn"),
    barnMellomnavn("barn[].mellomnavn"),
    barnEtternavn("barn[].etternavn"),
    barnFødselsdato("barn[].fødselsdato"),

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
    any { it.api != null && it.api.startsWith("barn[].") }
internal fun Set<Attributt>.etterspurtArbeidsgibereOrganaisasjoner() =
    any { it.api != null && it.api.startsWith("arbeidsgivere[].organisasjoner[]") }

private val megAttributter = setOf(
    Attributt.aktørId,
    Attributt.fornavn,
    Attributt.mellomnavn,
    Attributt.etternavn,
    Attributt.fødselsdato
)
internal fun Set<Attributt>.etterspurtMeg() = any { it in megAttributter }

internal data class OppslagResultat(
    internal val meg: Meg?,
    internal val barn: Set<Barn>?,
    internal val arbeidsgivereOrganisasjoner: Set<ArbeidsgiverOrganisasjon>?
)