package no.nav.k9.utgaende.rest

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import io.ktor.client.request.*
import io.ktor.http.*
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.clients.pdl.generated.HentBarn
import no.nav.k9.clients.pdl.generated.HentIdent
import no.nav.k9.clients.pdl.generated.HentPerson
import no.nav.k9.clients.pdl.generated.ID
import no.nav.k9.clients.pdl.generated.enums.IdentGruppe
import no.nav.k9.clients.pdl.generated.hentbarn.HentPersonBolkResult
import no.nav.k9.clients.pdl.generated.hentident.IdentInformasjon
import no.nav.k9.clients.pdl.generated.hentperson.Person
import no.nav.k9.inngaende.idToken
import no.nav.k9.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.coroutines.coroutineContext

class PDLProxy(
    private val client: GraphQLKtorClient,
    val accessTokenClient: AccessTokenClient,
    private val henteNavnScopes: Set<String> = setOf("openid"),
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PDLProxy::class.java)
    }

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)

    suspend fun person(ident: String): Person {
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
                client.execute(HentPerson(HentPerson.Variables(ident))) {
                    headers {
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }
                    header(NavHeaders.ConsumerToken,
                        cachedAccessTokenClient.getAccessToken(henteNavnScopes).asAuthoriationHeader())
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

    suspend fun barn(identer: List<ID>): List<HentPersonBolkResult> {

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
                client.execute(HentBarn(HentBarn.Variables(identer))) {
                    headers {
                        header(HttpHeaders.Authorization,
                            cachedAccessTokenClient.getAccessToken(henteNavnScopes).asAuthoriationHeader())
                        header(NavHeaders.ConsumerToken,
                            cachedAccessTokenClient.getAccessToken(henteNavnScopes).asAuthoriationHeader())
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
                    val errorSomJson = objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result)
                    logger.error("Feil ved henting av person-bolk. Årsak: {}", errorSomJson)
                    throw IllegalStateException("Feil ved henting av person-bolk.")
                }
            }
        }
    }

    suspend fun aktørId(ident: String): List<IdentInformasjon> {
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
                client.execute(HentIdent(HentIdent.Variables(ident, listOf(IdentGruppe.AKTORID), false))) {
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
