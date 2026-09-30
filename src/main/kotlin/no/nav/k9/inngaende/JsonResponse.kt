package no.nav.k9.inngaende

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

internal val jsonUtf8: MediaType = MediaType("application", "json", Charsets.UTF_8)

internal fun <T : Any> json(body: T): ResponseEntity<T> =
    ResponseEntity.ok().contentType(jsonUtf8).body(body)
