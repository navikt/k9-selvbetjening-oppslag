package no.nav.k9.inngaende.oppslag

import no.nav.tjeneste.virksomhet.person.v3.informasjon.Person

internal data class Fødselsnummer(internal val value: String)

internal enum class Attributt(internal val api: String) {
    fornavn("fornavn"),
    mellomnavn("mellomnavn"),
    etternavn("etternavn"),
    barnFornavn("barn.fornavn"),
    barnMellomnavn("barn.mellomnavn"),
    barnEtternavn("barn.etternavn");

    internal companion object {
        internal fun fraApi(api: String) : Attributt {
            for (value in values()) {
                if (value.api == api) return value
            }
            throw IllegalStateException("$api er ikke en støttet attributt.")
        }
    }
}

internal data class OppslagResultat(internal val personV3: Person?)