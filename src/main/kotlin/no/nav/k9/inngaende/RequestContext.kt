package no.nav.k9.inngaende

import kotlinx.coroutines.asContextElement

internal data class RequestContext(
    val correlationId: CorrelationId,
    val idToken: IdToken
)

internal class RequestContextService {

    private companion object {
        // https://github.com/Kotlin/kotlinx.coroutines/blob/master/docs/coroutine-context-and-dispatchers.md#thread-local-data
        private val requestContexts = ThreadLocal<RequestContext>()
    }

    internal fun getCoroutineContext(
        correlationId: CorrelationId,
        idToken: IdToken
    ) = requestContexts.asContextElement(
        RequestContext(
            correlationId,
            idToken
        )
    )

    private fun getRequestContext() = requestContexts.get() ?: throw IllegalStateException("RequestContext==null")

    internal fun getIdToken() = getRequestContext().idToken
    internal fun getCorrelationId() = getRequestContext().correlationId

}