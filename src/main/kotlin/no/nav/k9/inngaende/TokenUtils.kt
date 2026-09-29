package no.nav.k9.inngaende

import no.nav.security.token.support.core.context.TokenValidationContextHolder

private const val CLAIM_PID = "pid"
private const val CLAIM_SUB = "sub"

// Identen ligger i pid-claimet for tokenx-tokens fra ID-porten, med sub som fallback
internal fun TokenValidationContextHolder.personIdent(): String {
    val jwtToken = getTokenValidationContext().firstValidToken
        ?: throw IllegalStateException("Ingen gyldige tokens i Authorization headeren")

    val pid = jwtToken.jwtTokenClaims.getStringClaim(CLAIM_PID)
    val sub = jwtToken.jwtTokenClaims.getStringClaim(CLAIM_SUB)

    return when {
        !pid.isNullOrBlank() -> pid
        !sub.isNullOrBlank() -> sub
        else -> throw IllegalStateException("Ugyldig token. Token inneholdt verken sub eller pid claim")
    }
}
