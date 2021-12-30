package no.nav.k9.inngaende

import kotlinx.coroutines.asContextElement
import no.nav.helse.dusseldorf.ktor.auth.IdToken
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

// For bruk i suspending functions
// https://blog.tpersson.io/2018/04/22/emulating-request-scoped-objects-with-kotlin-coroutines/
private class CoroutineRequestContext(
    internal val correlationId: CorrelationId,
    internal val idToken: IdToken
) : AbstractCoroutineContextElement(Key) {
    internal companion object Key : CoroutineContext.Key<CoroutineRequestContext>
}

private fun CoroutineContext.requestContext() =
    get(CoroutineRequestContext.Key) ?: throw IllegalStateException("Request Context ikke satt.")
internal fun CoroutineContext.correlationId() = requestContext().correlationId
internal fun CoroutineContext.idToken() = requestContext().idToken


// For bruk i non suspending functions
// https://github.com/Kotlin/kotlinx.coroutines/blob/master/docs/coroutine-context-and-dispatchers.md#thread-local-data
internal class RequestContextService {

    private companion object {
        private val requestContexts = ThreadLocal<RequestContext>()
    }

    internal fun getCoroutineContext(
        context: CoroutineContext,
        correlationId: CorrelationId,
        idToken: IdToken
    ) = context + requestContexts.asContextElement(
        RequestContext(
            correlationId,
            idToken
        )
    ) + CoroutineRequestContext(
            correlationId,
            idToken
    )

    private fun getRequestContext() = requestContexts.get() ?: throw IllegalStateException("Request Context ikke satt.")
    internal fun getIdToken() = getRequestContext().idToken
    internal fun getCorrelationId() = getRequestContext().correlationId

    internal data class RequestContext(
        val correlationId: CorrelationId,
        val idToken: IdToken
    )
}
