package no.nav.k9.utgaende.rest

import org.slf4j.LoggerFactory
import java.time.LocalDate

data class PdlPerson(
    internal val fornavn: String,
    internal val mellomnavn: String?,
    internal val etternavn: String,
    internal val fødselsdato: LocalDate,
    internal val kontonummer: String?
)

data class PersonPdl(
    val `data`: Data
) {
    val log = LoggerFactory.getLogger(PersonPdl::class.java)

    data class Data(
        val hentPerson: HentPerson
    ) {
        data class HentPerson(
            val folkeregisteridentifikator: List<Folkeregisteridentifikator>,
            val navn: List<Navn>,
            val kjoenn: List<Kjoenn>,
            val doedsfall:List<Doedsfall>
        ) {
            data class Kjoenn(
                val kjoenn: String
            )

            data class Doedsfall(
                val doedsdato: LocalDate
            )

            data class Folkeregisteridentifikator(
                val identifikasjonsnummer: String
            )

            data class Navn(
                val etternavn: String,
                val forkortetNavn: String?,
                val fornavn: String,
                val mellomnavn: String?
            )
        }
    }
}
internal fun PersonPdl.navn(): String {
   return if(data.hentPerson.navn.isNotEmpty()) data.hentPerson.navn[0].forkortetNavn?:data.hentPerson.navn[0].fornavn + " " + data.hentPerson.navn[0].etternavn else "Uten navn"
}

internal fun PersonPdl.fnr(): String {
    return if(data.hentPerson.folkeregisteridentifikator.isNotEmpty()) data.hentPerson.folkeregisteridentifikator[0].identifikasjonsnummer else "Ukjent fnummer"
}

internal fun PersonPdl.kjoenn(): String {
    return if(data.hentPerson.kjoenn.isNotEmpty()) data.hentPerson.kjoenn[0].kjoenn else "Uten kjønn"
}
