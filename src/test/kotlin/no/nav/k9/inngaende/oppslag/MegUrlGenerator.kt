package no.nav.k9.inngaende.oppslag

import io.ktor.http.Url
import no.nav.helse.dusseldorf.ktor.client.buildURL
import java.net.URI
import java.time.LocalDate

object MegUrlGenerator {

    internal fun medAlleAttributter() = get(
        attributter = Attributt.values(),
        fraOgmed = LocalDate.now().minusDays(7),
        tilOgMed = LocalDate.now()
    )

    internal fun get(
        baseUrl: URI = URI("http://localhost:8080"),
        attributter: Array<Attributt>,
        fraOgmed: LocalDate?,
        tilOgMed: LocalDate?
    ) : URI {
        val queries = mutableMapOf("a" to attributter.map { it.api })
        fraOgmed?.apply { queries["fom"] = listOf(this.toString()) }
        tilOgMed?.apply { queries["tom"] = listOf(this.toString()) }

        return Url.buildURL(
            baseUrl = baseUrl,
            pathParts = listOf("meg"),
            queryParameters = queries
        )
    }
}
