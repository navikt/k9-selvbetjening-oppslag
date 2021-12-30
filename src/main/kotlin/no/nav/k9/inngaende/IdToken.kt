package no.nav.k9.inngaende

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.application.ApplicationCall
import io.ktor.auth.parseAuthorizationHeader
import no.nav.k9.inngaende.oppslag.Ident

internal data class IdToken(
    internal val value: String,
    internal val ident : Ident = Ident(subject(value))

) {

    companion object {
        fun subject(value: String): String {
            val token: DecodedJWT = JWT.decode(value)
            val issuer = token.issuer.lowercase()
            return when {
                issuer.contains("b2clogin") || issuer.contains("login-service") -> token.claims["sub"]?.asString() ?: throw IllegalStateException("Token mangler 'sub' claim.")
                issuer.contains("idporten") -> token.claims["pid"]?.asString() ?: throw IllegalStateException("Token mangler 'pid' claim.")
                else -> throw IllegalStateException("${token.issuer} er ukjent.")
            }
        }
    }
}

internal fun ApplicationCall.idToken() : IdToken {
    val jwt = request.parseAuthorizationHeader()?.render() ?: throw IllegalStateException("Token ikke satt")
    return IdToken(jwt.substringAfter("Bearer "))
}
