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
    val clients: Map<String, Client>,
) {
    companion object {
        private const val TOKEN_X = "tokenx"
        private const val AZURE = "azure"
    }

    internal val tokenxExchangeTokenClient get() = createSignedJwtAccessTokenClient(resolveClient(TOKEN_X))
    internal val azureSystemTokenClient get() = createSignedJwtAccessTokenClient(resolveClient(AZURE))

    private fun resolveClient(alias: String) =
        clients.getOrElse(alias) {
            throw IllegalStateException("Client[${alias}] må være satt opp.")
        } as PrivateKeyClient

    private fun resolveKeyId(client: PrivateKeyClient) = try {
        val jwk = JWK.parse(client.privateKeyJwk)
        requireNotNull(jwk.keyID) { "Private JWK inneholder ikke keyID." }
        jwk.keyID
    } catch (_: Throwable) {
        throw IllegalArgumentException("Private JWK på feil format.")
    }

    private fun createSignedJwtAccessTokenClient(client: PrivateKeyClient) = SignedJwtAccessTokenClient(
        clientId = client.clientId(),
        tokenEndpoint = client.tokenEndpoint(),
        privateKeyProvider = FromJwk(client.privateKeyJwk),
        keyIdProvider = DirectKeyId(resolveKeyId(client))
    )
}
