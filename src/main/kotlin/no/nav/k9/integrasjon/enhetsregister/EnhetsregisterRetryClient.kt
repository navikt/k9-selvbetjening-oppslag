package no.nav.k9.integrasjon.enhetsregister

import no.nav.k9.integrasjon.common.NavHeaderValues
import no.nav.k9.integrasjon.common.NavHeaders
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

@Component
@Retryable(
    excludes = [HttpClientErrorException::class],
    maxRetriesString = "\${spring.rest.retry.maxRetries}",
    delayString = "\${spring.rest.retry.initialDelay}",
    multiplierString = "\${spring.rest.retry.multiplier}",
    maxDelayString = "\${spring.rest.retry.maxDelay}",
)
internal class EnhetsregisterRetryClient(
    @Qualifier("enhetsregisterKlient") private val restClient: RestClient,
) {
    fun nøkkelinfo(path: String, organisasjonsnummer: String, callId: String): String =
        restClient.get()
            .uri(path, organisasjonsnummer)
            .accept(MediaType.APPLICATION_JSON)
            .header(NavHeaders.ConsumerId, NavHeaderValues.ConsumerId)
            .header(NavHeaders.CallId, callId)
            .retrieve()
            .body(String::class.java)
            ?: throw IllegalStateException("Tom respons ved henting av Nøkkelinfo for organisasjon $organisasjonsnummer")
}
