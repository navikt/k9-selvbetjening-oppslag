package no.nav.k9.utgaende.rest

import com.expediagroup.graphql.client.GraphQLKtorClient
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.core.Violation
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.HentIdent
import no.nav.k9.HentPerson
import no.nav.k9.inngaende.idToken
import no.nav.k9.objectMapper
import no.nav.k9.utils.Cache
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.util.*
import kotlin.coroutines.coroutineContext

class PDLProxy(
    val baseUrl: URI,
    val accessTokenClient: AccessTokenClient,
    private val henteNavnScopes: Set<String> = setOf("openid"),
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PDLProxy::class.java)
    }

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val cache = Cache<String>(10_000)

    @KtorExperimentalAPI
    private val client = GraphQLKtorClient(
        url = baseUrl.toURL(),
        mapper = objectMapper()
    ) {
        engine {
            this.requestTimeout
        }
        request {
            headers {
                header(
                    NavHeaders.ConsumerToken,
                    cachedAccessTokenClient.getAccessToken(henteNavnScopes).asAuthoriationHeader()
                )
                header(NavHeaders.Tema, "OMS")
            }
        }
    }

    @KtorExperimentalAPI
    suspend fun person(ident: String): HentPerson.Person {
        val token = coroutineContext.idToken().value

        val hentPersonRequest = HentPerson(client.apply {
            request {
                headers {
                    header(NavHeaders.CallId, UUID.randomUUID().toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        })

        return Retry.retry(
            operation = "hente-person",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val result = Operation.monitored(
                app = "k9-selvbetjening-oppslag",
                operation = "hente-person",
                resultResolver = { it.errors.isNullOrEmpty() }
            ) { hentPersonRequest.execute(HentPerson.Variables(ident)) }

            when {
                result.data!!.hentPerson != null -> result.data!!.hentPerson!!
                !result.errors.isNullOrEmpty() -> {
                    val errorSomJson = objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result.data)
                    logger.info("Feil ved henting av person. Årsak: {}", errorSomJson)
                    throw IllegalStateException("Feil ved henting av person.")
                }
                else -> {
                    logger.error("Hva skjer her?? {}", result)
                    throw IllegalStateException("Feil ved henting av person.")
                }
            }
        }
    }

    @KtorExperimentalAPI
    suspend fun aktørId(ident: String): List<HentIdent.IdentInformasjon> {
        val token = coroutineContext.idToken().value

        val hentPersonRequest = HentIdent(client.apply {
            request {
                headers {
                    header(NavHeaders.CallId, UUID.randomUUID().toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        })

        return Retry.retry(
            operation = "hente-person",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val result = Operation.monitored(
                app = "k9-selvbetjening-oppslag",
                operation = "hente-person",
                resultResolver = { it.errors.isNullOrEmpty() }
            ) { hentPersonRequest.execute(HentIdent.Variables(ident, listOf(HentIdent.IdentGruppe.AKTORID))) }

            when {
                !result.errors.isNullOrEmpty() -> {
                    val errorSomJson = objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result.data)
                    logger.info("Feil ved henting av person. Årsak: {}", errorSomJson)
                    throw IllegalStateException("Feil ved henting av person.")
                }
                result.data!!.hentIdenter!!.identer.isNullOrEmpty() -> result.data!!.hentIdenter!!.identer
                else -> {
                    logger.error("Hva skjer her?? {}", result)
                    throw IllegalStateException("Feil ved henting av person.")
                }
            }
        }
    }
}
