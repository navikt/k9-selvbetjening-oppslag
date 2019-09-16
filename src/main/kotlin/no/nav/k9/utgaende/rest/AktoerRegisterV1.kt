package no.nav.k9.utgaende.rest

import io.ktor.http.Url
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.k9.inngaende.oppslag.Fødselsnummer
import java.net.URI

internal class AktørRegisterV1(
    baseUrl: URI
) {

    private val aktoerIdUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf("api","v1","identer"),
        queryParameters = mapOf(
            Pair("gjeldende", listOf("true")),
            Pair("identgruppe", listOf("AktoerId"))
        )
    ).toString()



    internal suspend fun aktørId(fødselsnummer: Fødselsnummer) : AktørId{
        return AktørId(value = "1234")
    }

}


data class AktørId(internal val value: String)