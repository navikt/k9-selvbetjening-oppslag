package no.nav.k9.inngaende

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.jvm.javaio.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.text.Charsets

internal class JsonConverter : ContentConverter {
    override suspend fun deserialize(charset: Charset, typeInfo: TypeInfo, content: ByteReadChannel): Any {
        return content.toInputStream().bufferedReader().readText()
    }

    @Deprecated("Deprecated in io.ktor.serialization.ContentConverter")
    override suspend fun serialize(
        contentType: ContentType,
        charset: Charset,
        typeInfo: TypeInfo,
        value: Any
    ): OutgoingContent? {
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
