package no.nav.k9.inngaende

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

// For bruk i suspending functions
// https://blog.tpersson.io/2018/04/22/emulating-request-scoped-objects-with-kotlin-coroutines/
internal class CoroutineRequestContext(
    internal val correlationId: CorrelationId,
) : AbstractCoroutineContextElement(Key) {
    internal companion object Key : CoroutineContext.Key<CoroutineRequestContext>
}

private fun CoroutineContext.requestContext() =
    get(CoroutineRequestContext.Key) ?: throw IllegalStateException("Request Context ikke satt.")

internal fun CoroutineContext.correlationId() = requestContext().correlationId
