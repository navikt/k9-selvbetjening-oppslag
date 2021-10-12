package no.nav.k9.utgaende.gateway

import no.nav.helse.dusseldorf.oauth2.client.CachedAccessTokenClient
import no.nav.k9.clients.pdl.generated.hentbarn.HentPersonBolkResult
import no.nav.k9.clients.pdl.generated.hentperson.Person
import no.nav.k9.clients.pdl.generated.hentident.IdentInformasjon
import no.nav.k9.inngaende.idToken
import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.inngaende.oppslag.Ident
import no.nav.k9.inngaende.oppslag.OppslagService.Companion.støttedeAttributter
import no.nav.k9.utgaende.rest.PDLProxy
import no.nav.siftilgangskontroll.core.tilgang.BarnTilgangForespørsel
import no.nav.siftilgangskontroll.core.tilgang.TilgangService
import no.nav.siftilgangskontroll.policy.spesification.PolicyDecision
import org.slf4j.LoggerFactory
import kotlin.coroutines.coroutineContext

class PDLProxyGateway(
    private val pdlProxy: PDLProxy,
    private val tilgangService: TilgangService,
    private val cachedAccessTokenClient: CachedAccessTokenClient,
    private val cachedSystemTokenClient: CachedAccessTokenClient,
    private val pdlApiTokenxAudience: String,
    private val pdlApiAzureAudience: String
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(PDLProxyGateway::class.java)
    }

    @Throws(TilgangNektetException::class)
    internal suspend fun person(
        ident: Ident,
    ): Person {
        val exchangeToken = cachedAccessTokenClient.getAccessToken(
            scopes = setOf(pdlApiTokenxAudience),
            onBehalfOf = coroutineContext.idToken().value)

        val tilgangResponse = tilgangService.hentPerson(exchangeToken.token)
        return when (tilgangResponse.policyEvaluation.decision) {
            PolicyDecision.PERMIT -> pdlProxy.person(ident.value)
            else -> {
                logger.error("Tilgang til person nektet. Grunn: {}", tilgangResponse.policyEvaluation)
                throw TilgangNektetException("Tilgang til person nektet")
            }
        }
    }

    internal suspend fun barn(
        identer: List<Ident>,
    ): List<HentPersonBolkResult> {
        val identListe = identer.map { it.value }
        val exchangeToken = cachedAccessTokenClient.getAccessToken(
            scopes = setOf(pdlApiTokenxAudience),
            onBehalfOf = coroutineContext.idToken().value)

        val systemToken = cachedSystemTokenClient.getAccessToken(setOf(pdlApiAzureAudience))
        val tilgangResponse =
            tilgangService.hentBarn(
                BarnTilgangForespørsel(identListe),
                exchangeToken.token,
                systemToken.token
            )

        val tillatteIdenter = tilgangResponse
            .filter { it.policyEvaluation.decision == PolicyDecision.PERMIT }
            .map { it.ident }

        return if (tillatteIdenter.isEmpty()) emptyList()
        else pdlProxy.barn(tillatteIdenter)
    }

    internal suspend fun aktørId(
        ident: Ident,
        attributter: Set<Attributt>,
    ): List<IdentInformasjon>? = when {
        attributter.any { it in støttedeAttributter } -> pdlProxy.aktørId(ident.value)
        else -> null
    }
}

data class TilgangNektetException(override val message: String) : RuntimeException(message)
