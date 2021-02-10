package no.nav.k9.utgaende.rest

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpPost
import io.ktor.http.*
import io.ktor.util.*
import no.nav.helse.dusseldorf.ktor.client.buildURL
import no.nav.helse.dusseldorf.ktor.core.Retry
import no.nav.helse.dusseldorf.ktor.metrics.Operation
import no.nav.helse.dusseldorf.oauth2.client.AccessTokenClient
import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.inngaende.idToken
import no.nav.k9.objectMapper
import no.nav.k9.utils.Cache
import no.nav.k9.utils.CacheObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.time.LocalDateTime
import java.util.*
import kotlin.coroutines.coroutineContext

class PDLProxy(
    val baseUrl: URI,
    val accessTokenClient: AccessTokenClient,
    private val henteNavnScopes: Set<String> = setOf("openid")
) {

    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(PDLProxy::class.java)
    }

    private val cachedAccessTokenClient = CachedAccessTokenClient(accessTokenClient)
    private val cache = Cache<String>(10_000)

    private val personUrl = Url.buildURL(
        baseUrl = baseUrl,
        pathParts = listOf()
    ).toString()

    @KtorExperimentalAPI
    suspend fun person(aktorId: String): PersonPdlResponse {
        val queryRequest = QueryRequest(
            getStringFromResource("/pdl/hentPerson.graphql"),
            mapOf("ident" to aktorId)
        )
        val query = objectMapper().writeValueAsString(queryRequest)

        val cachedObject = cache.get(query)
        return if (cachedObject == null) {
            val callId = UUID.randomUUID().toString()
            val httpRequest = personUrl
                .httpPost()
                .body(
                    query
                )
                .header(
                    HttpHeaders.Authorization to "Bearer ${coroutineContext.idToken().value}",
                    NavHeaders.ConsumerToken to cachedAccessTokenClient.getAccessToken(henteNavnScopes)
                        .asAuthoriationHeader(),
                    HttpHeaders.Accept to "application/json",
                    HttpHeaders.ContentType to "application/json",
                    NavHeaders.Tema to "OMS",
                    NavHeaders.CallId to callId
                )

            val json: PersonPdlResponse = Retry.retry(
                operation = "hente-person",
                initialDelay = Duration.ofMillis(200),
                factor = 2.0,
                logger = logger
            ) {
                val (request, _, result) = Operation.monitored(
                    app = "k9-los-api",
                    operation = "hente-person",
                    resultResolver = { 200 == it.second.statusCode }
                ) { httpRequest.awaitStringResponseResult() }

                result.fold(
                    { success ->
                        val personPdl = objectMapper().readValue<PersonPdl>(success)
                        cache.set(query, CacheObject(success, LocalDateTime.now().plusHours(7)))

                        PersonPdlResponse(false, personPdl)
                    },
                    { error ->
                        logger.warn(
                            "Error response = '${error.response.body().asString("text/plain")}' fra '${request.url}'"
                        )

                        val value = objectMapper().readValue<Error>(error.errorData)
                        if (value.errors.any { it.extensions.code == "unauthorized" }) {
                            PersonPdlResponse(true, null)
                        }

                        logger.warn(error.toString() + "aktorId callId: " + callId)
                        throw IllegalStateException("Feil ved henting av person.")
                    }
                )
            }
            return json
        }
        else PersonPdlResponse(false, objectMapper().readValue<PersonPdl>(cachedObject.value))
    }

    data class QueryRequest(
        val query: String,
        val variables: Map<String, Any>,
        val operationName: String? = null
    ) {
        data class Variables(
            val variables: Map<String, Any>
        )
    }

    private fun getStringFromResource(path: String) =
        PDLProxy::class.java.getResourceAsStream(path).bufferedReader().use { it.readText() }
}

data class PersonPdlResponse(
    val ikkeTilgang: Boolean,
    val person: PersonPdl?
)
