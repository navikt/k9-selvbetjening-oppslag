package no.nav.k9.utgaende.auth

import com.nimbusds.jose.jwk.JWK
import no.nav.helse.dusseldorf.ktor.auth.Client
import no.nav.helse.dusseldorf.ktor.auth.PrivateKeyClient
import no.nav.helse.dusseldorf.oauth2.client.DirectKeyId
import no.nav.helse.dusseldorf.oauth2.client.FromJwk
import no.nav.helse.dusseldorf.oauth2.client.SignedJwtAccessTokenClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class AccessTokenClientResolver(
    clients: Map<String, Client>
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(AccessTokenClientResolver::class.java)
        private const val TOKEN_X_PDL_API = "tokenx-pdl-api"
    }

    private val tokenxPdlApiClient = clients.getOrElse(TOKEN_X_PDL_API) {
        throw IllegalStateException("Client[$TOKEN_X_PDL_API] må være satt opp.")
    } as PrivateKeyClient

    private val keyId = try {
        val jwk = JWK.parse(tokenxPdlApiClient.privateKeyJwk)
        requireNotNull(jwk.keyID) { "Azure JWK inneholder ikke keyID." }
        jwk.keyID
    } catch (_: Throwable) {
        throw IllegalArgumentException("Azure JWK på feil format.")
    }

    private val tokenxPdlApiExchangeTokenClient = SignedJwtAccessTokenClient(
        clientId = tokenxPdlApiClient.clientId(),
        tokenEndpoint = tokenxPdlApiClient.tokenEndpoint(),
        privateKeyProvider = FromJwk(tokenxPdlApiClient.privateKeyJwk),
        keyIdProvider = DirectKeyId(keyId)
    )

    internal fun tokenxPdlApiExchangeTokenClient() = tokenxPdlApiExchangeTokenClient
}
