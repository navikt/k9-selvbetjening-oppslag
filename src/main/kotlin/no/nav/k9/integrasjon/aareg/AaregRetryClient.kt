package no.nav.k9.integrasjon.aareg

import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.integrasjon.common.NavHeaders
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient

internal const val ARBEIDSFORHOLD_PATH = "/arbeidstaker/arbeidsforhold"

@Component
@Retryable(
    excludes = [
        ResourceAccessException::class,
        HttpClientErrorException::class,
    ],
    maxRetriesString = "\${spring.rest.retry.maxRetries}",
    delayString = "\${spring.rest.retry.initialDelay}",
    multiplierString = "\${spring.rest.retry.multiplier}",
    maxDelayString = "\${spring.rest.retry.maxDelay}",
)
internal class AaregRetryClient(
    @Qualifier("aaregKlient") private val restClient: RestClient,
) {
    fun arbeidsforhold(
        token: String,
        callId: String,
        ident: Ident,
    ): String =
        restClient.get()
            .uri {
                it.path(ARBEIDSFORHOLD_PATH)
                    .queryParam("arbeidsforholdtype", ArbeidsforholdType.somQueryParameters())
                    .queryParam("arbeidsforholdstatus", ArbeidsforholdStatus.somQueryParameters())
                    .build()
            }
            .headers { it.setBearerAuth(token) }
            .accept(MediaType.APPLICATION_JSON)
            .header(NavHeaders.CallId, callId)
            .header(NavHeaders.PersonIdent, ident.value)
            .retrieve()
            .body(String::class.java)
            ?: throw IllegalStateException("Tom respons ved henting av arbeidsforhold per arbeidstaker")
}
