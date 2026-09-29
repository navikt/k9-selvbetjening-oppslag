package no.nav.k9.integrasjon.enhetsregister

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import java.net.URI

// Timeout (15 s) settes med spring.http.clients.connect-timeout og read-timeout i application.yml.
@Configuration
internal class EnhetsregisterClientConfig {

    @Bean("enhetsregisterKlient")
    fun enhetsregisterKlient(
        restClientBuilder: RestClient.Builder,
        @Value("\${nav.register-urls.enhetsregister-v1}") baseUrl: URI,
    ): RestClient = restClientBuilder.baseUrl(baseUrl.toString().trimEnd('/')).build()
}
