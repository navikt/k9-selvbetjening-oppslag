package no.nav.k9.utgaende.rest

import org.json.JSONArray
import org.json.JSONException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert

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

    @Test
    internal fun `Direkte parsing til JSONArray feiler`() {
        assertThrows<JSONException> { JSONArray(json) }
    }

    @Test
    internal fun `Parsing til JSONArray via utils fungerer`() {
        val result = json.somJsonArray()
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