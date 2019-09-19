package no.nav.k9.inngaende

import com.auth0.jwt.JWT
import io.ktor.application.ApplicationCall
import io.ktor.auth.parseAuthorizationHeader
import no.nav.k9.inngaende.oppslag.Ident

internal data class IdToken(
    internal val value: String,
    internal val ident : Ident = Ident(
        JWT.decode(value).subject ?: throw IllegalStateException("Token mangler 'sub' claim.")
    )
)

internal fun ApplicationCall.idToken() : IdToken {
    val jwt = request.parseAuthorizationHeader()?.render() ?: throw IllegalStateException("Token ikke satt")
    return IdToken(jwt.substringAfter("Bearer "))
}