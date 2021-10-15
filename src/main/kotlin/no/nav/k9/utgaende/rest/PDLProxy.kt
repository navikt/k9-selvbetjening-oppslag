package no.nav.k9.utgaende.rest

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.clients.pdl.generated.HentIdent
import no.nav.k9.clients.pdl.generated.enums.IdentGruppe
import no.nav.k9.clients.pdl.generated.hentident.IdentInformasjon
import no.nav.k9.inngaende.idToken
import no.nav.k9.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.coroutines.coroutineContext

class PDLProxy(
    private val pdlClient: GraphQLKtorClient,
    private val cachedAccessTokenClient: CachedAccessTokenClient,
    private val pdlApiTokenxAudience: String,
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PDLProxy::class.java)
    }

    suspend fun aktørId(ident: String): List<IdentInformasjon> {
        val exchangeToken = cachedAccessTokenClient.getAccessToken(
            scopes = setOf(pdlApiTokenxAudience),
            onBehalfOf = coroutineContext.idToken().value)

        return Retry.retry(
            operation = "hent-ident",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val result = Operation.monitored(
                app = "k9-selvbetjening-oppslag",
                operation = "hent-ident",
                resultResolver = { it.errors.isNullOrEmpty() }
            ) {
                pdlClient.execute(HentIdent(HentIdent.Variables(ident, listOf(IdentGruppe.AKTORID), false))) {
                    headers {
                        header(HttpHeaders.Authorization, exchangeToken.asAuthoriationHeader())
                    }
                }
            }

            when {
                !result.errors.isNullOrEmpty() -> {
                    val errorSomJson = objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result.errors)
                    logger.error("Feil ved henting av ident. Årsak: {}", errorSomJson)
                    throw IllegalStateException("Feil ved henting av ident.")
                }
                !result.data!!.hentIdenter!!.identer.isNullOrEmpty() -> result.data!!.hentIdenter!!.identer
                else -> {
                    throw IllegalStateException("Feil ved henting av ident.")
                }
            }
        }
    }
}
