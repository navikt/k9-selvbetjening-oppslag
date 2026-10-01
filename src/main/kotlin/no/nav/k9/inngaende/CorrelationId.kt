package no.nav.k9.inngaende

internal data class CorrelationId(internal val value: String)

internal object CorrelationIdVerifier {
    const val HEADER = "X-Correlation-ID"
    private val gyldigFormat = Regex("[a-zA-Z0-9_.\\-æøåÆØÅ]{5,200}")

    internal fun erGyldig(value: String) = gyldigFormat.matches(value)
}
