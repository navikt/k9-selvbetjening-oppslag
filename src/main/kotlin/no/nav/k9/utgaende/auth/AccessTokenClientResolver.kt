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
        private val logger: Logger = LoggerFactory.getLogger(AccessTokenClientResolver::class.java)
        private const val TOKEN_X_PDL_API = "tokenx-pdl-api"
        private const val AZURE_PDL_API = "azure-pdl-api"
    }

    internal val tokenxPdlApiExchangeTokenClient get() = createSignedJwtAccessTokenClient(resolveClient(TOKEN_X_PDL_API))
    internal val azurePdlApiSystemTokenClient get() = createSignedJwtAccessTokenClient(resolveClient(AZURE_PDL_API))

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
