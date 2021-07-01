package no.nav.k9.inngaende

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.application.ApplicationCall
import io.ktor.auth.parseAuthorizationHeader
import no.nav.k9.inngaende.oppslag.Ident

internal data class IdToken(
    internal val value: String,
    internal val ident : Ident = Ident(
        JWT.decode(value).subject ?: throw IllegalStateException("Token mangler 'sub' claim.")
    )
)

fun subject(value: String): String {
    val token: DecodedJWT = JWT.decode(value)
    return when {
        token.issuer.contains("tokendings") -> token.getClaim("pid").asString()
            ?: throw IllegalStateException("Token mangler 'pid' claim.")

        else -> token.subject ?: throw IllegalStateException("Token mangler 'sub' claim.")
    }
}

internal fun ApplicationCall.idToken(): IdToken {
    val jwt = request.parseAuthorizationHeader()?.render() ?: throw IllegalStateException("Token ikke satt")
    return IdToken(jwt.substringAfter("Bearer "))
}
