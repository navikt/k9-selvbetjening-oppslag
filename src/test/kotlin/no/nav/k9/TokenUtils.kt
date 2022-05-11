package no.nav.k9

import no.nav.security.mock.oauth2.MockOAuth2Server

object TokenUtils {
    fun MockOAuth2Server.hentToken(
        subject: String,
        issuerId: String = "tokendings",
        audience: String = "dev-fss:dusseldorf:k9-selvbetjening-oppslag",
        claims: Map<String, String> = mapOf("acr" to "Level4")
    ): String {
        return issueToken(issuerId = issuerId, subject = subject, audience = audience, claims = claims).serialize()
    }
}
