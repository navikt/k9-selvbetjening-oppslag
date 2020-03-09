package no.nav.k9.inngaende

import io.ktor.application.ApplicationCall
import io.ktor.features.ContentConverter
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.request.ApplicationReceiveRequest
import io.ktor.util.pipeline.PipelineContext
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import org.json.JSONArray
import org.json.JSONObject

internal class JsonConverter : ContentConverter {
    override suspend fun convertForReceive(context: PipelineContext<ApplicationReceiveRequest, ApplicationCall>): Any? {
        val request = context.subject
        val channel = request.value as? ByteReadChannel ?: return null
        return JSONObject(channel.toInputStream().bufferedReader().readText())
    }

    override suspend fun convertForSend(
        context: PipelineContext<Any, ApplicationCall>,
        contentType: ContentType,
        value: Any
    ): Any? {
        val json = when(value) {
            is Map<*, *> -> JSONObject(value).toString()
            is List<*> -> JSONArray(value).toString()
            is JSONObject -> value.toString()
            is JSONArray -> value.toString()
            else -> throw IllegalStateException("Ikke støttet type ${value.javaClass}")
        }
        return TextContent(json, contentType.withCharset(Charsets.UTF_8))
    }

}
