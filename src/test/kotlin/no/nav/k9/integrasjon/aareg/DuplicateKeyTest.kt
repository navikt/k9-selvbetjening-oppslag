package no.nav.k9.integrasjon.aareg

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper

internal class DuplicateKeyTest {

    private companion object {
        internal val json = """
            [
                {
                    "type": "1",
                    "type": "2"
                },
                {
                    "type": "3",
                    "type": "4"
                }
            ]
        """.trimIndent()
    }

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @Test
    internal fun `Parsing av duplikate nøkler bruker siste verdi i stedet for å feile`() {
        val result = objectMapper.readTree(json)
        JSONAssert.assertEquals("""
            [
                {
                    "type": "2"
                },
                {
                    "type": "4"
                }
            ]
        """.trimIndent(), result.toString(), true)
    }

}
