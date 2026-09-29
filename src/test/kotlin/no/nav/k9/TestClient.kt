package no.nav.k9

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

const val X_CORRELATION_ID = "X-Correlation-ID"

class TestResponse(val status: HttpStatus, val body: String, private val contentTypeHeader: String?) {
    val contentType: MediaType? get() = contentTypeHeader?.let { MediaType.parseMediaType(it) }
}

class TestRequest {
    internal val headers = linkedMapOf<String, String>()
    internal var body: String? = null

    fun header(name: String, value: String) {
        headers[name] = value
    }

    fun body(value: String) {
        body = value
    }
}

/** Enkel HTTP-klient mot appen som kjører på tilfeldig port. Spesialtegn i path og query kodes før sending. */
class TestClient(private val baseUrl: () -> String) {
    private val httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()

    fun get(pathAndQuery: String, block: TestRequest.() -> Unit = {}) = send("GET", pathAndQuery, block)
    fun post(pathAndQuery: String, block: TestRequest.() -> Unit = {}) = send("POST", pathAndQuery, block)

    private fun send(method: String, pathAndQuery: String, block: TestRequest.() -> Unit): TestResponse {
        val request = TestRequest().apply(block)
        val builder = HttpRequest.newBuilder(URI.create(baseUrl() + pathAndQuery.kod()))
            .method(method, HttpRequest.BodyPublishers.ofString(request.body ?: ""))
        request.headers.forEach { (name, value) -> builder.header(name, value) }

        val response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        return TestResponse(
            status = HttpStatus.valueOf(response.statusCode()),
            body = response.body(),
            contentTypeHeader = response.headers().firstValue("Content-Type").orElse(null)
        )
    }

    private fun String.kod(): String {
        val tillatt = { c: Char -> c.isLetterOrDigit() && c.code < 128 || c in "-._~!$&'()*+,;=:@/?%" }
        return buildString {
            for (c in this@kod) {
                if (tillatt(c)) append(c)
                else c.toString().toByteArray(StandardCharsets.UTF_8).forEach { append("%%%02X".format(it)) }
            }
        }
    }
}
