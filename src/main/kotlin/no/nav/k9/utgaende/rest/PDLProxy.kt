package no.nav.k9.utgaende.rest

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.*
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.clients.pdl.generated.HentIdent
import no.nav.k9.clients.pdl.generated.HentPerson
import no.nav.k9.clients.pdl.generated.HentPersonBolk
import no.nav.k9.clients.pdl.generated.ID
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
        mapper = objectMapper(),
        config = {
            defaultRequest {
                headers {
                    header(NavHeaders.Tema, "OMS")
                    header(NavHeaders.CallId, UUID.randomUUID().toString())
                    header(NavHeaders.ConsumerToken, cachedAccessTokenClient.getAccessToken(henteNavnScopes).asAuthoriationHeader())
                }
            }
        }
    )

    @KtorExperimentalAPI
    suspend fun person(ident: String): HentPerson.Person {
        val token = coroutineContext.idToken().value

        return Retry.retry(
            operation = "hent-person",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val result = Operation.monitored(
                app = "k9-selvbetjening-oppslag",
                operation = "hent-person",
                resultResolver = { it.errors.isNullOrEmpty() }
            ) {
                HentPerson(client).execute(HentPerson.Variables(ident)) {
                    headers {
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }
                }
            }

            when {
                result.data!!.hentPerson != null -> result.data!!.hentPerson!!
                !result.errors.isNullOrEmpty() -> {
                    val errorSomJson = objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result.errors)
                    logger.error("Feil ved henting av person. Årsak: {}", errorSomJson)
                    throw IllegalStateException("Feil ved henting av person.")
                }
                else -> {
                    throw IllegalStateException("Feil ved henting av person.")
                }
            }
        }
    }

    @KtorExperimentalAPI
    suspend fun personBolk(identer: List<ID>): List<HentPersonBolk.HentPersonBolkResult> {

        return Retry.retry(
            operation = "hent-person-bolk",
            initialDelay = Duration.ofMillis(200),
            factor = 2.0,
            logger = logger
        ) {
            val result = Operation.monitored(
                app = "k9-selvbetjening-oppslag",
                operation = "hent-person-bolk",
                resultResolver = { it.errors.isNullOrEmpty() }
            ) {
                HentPersonBolk(client).execute(HentPersonBolk.Variables(identer)) {
                    headers {
                        header(HttpHeaders.Authorization, cachedAccessTokenClient.getAccessToken(henteNavnScopes).asAuthoriationHeader())
                    }
                }
            }

            when {
                result.data!!.hentPersonBolk.isNotEmpty() -> result.data!!.hentPersonBolk
                !result.errors.isNullOrEmpty() -> {
                    val errorSomJson = objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result.errors)
                    logger.error("Feil ved henting av person-bolk. Årsak: {}", errorSomJson)
                    throw IllegalStateException("Feil ved henting av person-bolk.")
                }
                else -> {
                    throw IllegalStateException("Feil ved henting av person-bolk.")
                }
            }
        }
    }

    @KtorExperimentalAPI
    suspend fun aktørId(ident: String): List<HentIdent.IdentInformasjon> {
        val token = coroutineContext.idToken().value

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
                HentIdent(client).execute(HentIdent.Variables(ident, listOf(HentIdent.IdentGruppe.AKTORID), false))  {
                    headers {
                        header(HttpHeaders.Authorization, "Bearer $token")
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
