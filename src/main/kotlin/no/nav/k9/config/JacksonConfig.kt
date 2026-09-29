package no.nav.k9.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

// Jackson 2, med samme oppsett som appen hadde før (dusseldorfConfigured). Brukes til GraphQL-klienten og request body på /system.
internal fun k9ObjectMapper(): ObjectMapper = jacksonObjectMapper()
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
    .enable(SerializationFeature.INDENT_OUTPUT)
    .apply {
        propertyNamingStrategy = PropertyNamingStrategies.LOWER_CAMEL_CASE
        registerModule(JavaTimeModule())
    }
