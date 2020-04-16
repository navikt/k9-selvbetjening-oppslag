package no.nav.k9.utgaende.rest

import no.nav.k9.inngaende.oppslag.Ident
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate

internal class BrregProxyV1 {
    internal suspend fun foretak(
        ident: Ident
    ) : Set<Foretak> {

        val json = JSONObject()
        json.forsikreIkkeTekniskFeil()

        val roller = json.getJsonArrayOrEmpty("roller")

        if (roller.isEmpty) {
            logger.info("Har ikke noen roller i noen foretak.")
            return setOf()
        }

        val foretak = roller.map { it as JSONObject }.map {
            Foretak(
                organisasjonsnummer = it.getString("organisasjonsnummer"),
                registreringsdato = LocalDate.parse(it.getString("registreringsDato")),
                rollebeskrivelse = it.getString("rollebeskrivelse")
            )
        }.toMutableSet()

        val antallForetakFørFiltrering = foretak.size
        val filtrert = foretak.filtrerPåStatuskoder(json)
        if (antallForetakFørFiltrering != filtrert.size) {
            logger.info("Filtrert fra $antallForetakFørFiltrering til ${filtrert.size} foretak.")
        }
        return filtrert
    }

    private companion object {
        internal val logger: Logger = LoggerFactory.getLogger(BrregProxyV1::class.java)
    }
}

internal data class Foretak(
    val organisasjonsnummer: String,
    val registreringsdato: LocalDate,
    val rollebeskrivelse: String
)