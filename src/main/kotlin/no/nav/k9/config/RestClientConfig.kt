package no.nav.k9.config

import no.nav.k9.utgaende.auth.AaregAuthService
import no.nav.k9.utgaende.gateway.ArbeidsgiverOgArbeidstakerRegisterGateway
import no.nav.k9.utgaende.gateway.EnhetsregisterV1Gateway
import no.nav.k9.utgaende.rest.EnhetsregisterV1
import no.nav.k9.utgaende.rest.aaregv2.ArbeidsgiverOgArbeidstakerRegisterV2
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener
import org.springframework.retry.support.RetryTemplate
import org.springframework.web.client.RestClient
import java.net.URI

// Timeout (15 s) settes med spring.http.clients.connect-timeout og read-timeout i application.yml.
@Configuration
internal class RestClientConfig {

    private val logger = LoggerFactory.getLogger(RestClientConfig::class.java)

    // 3 forsøk, 200 ms ventetid som dobles opp til maks 1 s
    @Bean("registerRetryTemplate")
    fun retryTemplate(): RetryTemplate = RetryTemplate.builder()
        .maxAttempts(3)
        .exponentialBackoff(200, 2.0, 1000)
        .withListener(object : RetryListener {
            override fun <T : Any?, E : Throwable?> onError(
                context: RetryContext,
                callback: RetryCallback<T, E>,
                throwable: Throwable,
            ) {
                logger.warn(
                    "Feil ved oppslag, forsøk ${context.retryCount} av 3. ${throwable.javaClass.simpleName}: ${throwable.message}"
                )
            }
        })
        .build()

    @Bean("enhetsregisterV1Gateway")
    internal fun enhetsregisterV1Gateway(
        restClientBuilder: RestClient.Builder,
        registerRetryTemplate: RetryTemplate,
        @Value("\${nav.register-urls.enhetsregister-v1}") baseUrl: URI,
    ) = EnhetsregisterV1Gateway(
        enhetsregisterV1 = EnhetsregisterV1(
            baseUrl = baseUrl,
            restClientBuilder = restClientBuilder,
            retryTemplate = registerRetryTemplate
        )
    )

    @Bean("arbeidsgiverOgArbeidstakerRegisterGateway")
    internal fun arbeidsgiverOgArbeidstakerRegisterGateway(
        restClientBuilder: RestClient.Builder,
        registerRetryTemplate: RetryTemplate,
        aaregAuthService: AaregAuthService,
        @Value("\${nav.register-urls.arbeidsgiver-og-arbeidstaker-v2}") baseUrl: URI,
    ) = ArbeidsgiverOgArbeidstakerRegisterGateway(
        arbeidstakerOgArbeidstakerRegisterV2 = ArbeidsgiverOgArbeidstakerRegisterV2(
            baseUrl = baseUrl,
            restClientBuilder = restClientBuilder,
            retryTemplate = registerRetryTemplate,
            aaregAuthService = aaregAuthService
        )
    )
}
