package no.nav.k9.utgaende.gateway

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MetricsFriendlyTest {

    @Test
    fun `Roller fra Brønnøysund`() {
        val rollerOgForventetUtfall = mapOf(
            "Bestyrende reder" to "BESTYRENDEREDER",
            "Daglig leder/ adm.direktør" to "DAGLIGLEDERADMDIREKTR",
            "Deltaker med delt ansvar" to "DELTAKERMEDDELTANSVAR",
            "Deltaker med fullt ansvar" to "DELTAKERMEDFULLTANSVAR",
            "Forretningsfører" to "FORRETNINGSFRER",
            "Innehaver" to "INNEHAVER",
            "Komplementar" to "KOMPLEMENTAR",
            "Kontaktperson" to "KONTAKTPERSON",
            "Styrets leder" to "STYRETSLEDER",
            "Styremedlem" to "STYREMEDLEM",
            "Nestleder" to "NESTLEDER",
            "Observatør" to "OBSERVATR",
            "Prokura i fellesskap" to "PROKURAIFELLESSKAP",
            "Prokura hver for seg" to "PROKURAHVERFORSEG",
            "Prokura" to "PROKURA",
            "Norsk repr. for utenl. enhet" to "NORSKREPRFORUTENLENHET",
            "Signatur i fellesskap" to "SIGNATURIFELLESSKAP",
            "Signatur" to  "SIGNATUR",
            "Signatur hver for seg" to "SIGNATURHVERFORSEG",
            "Varamedlem" to "VARAMEDLEM",
            "En r@r roll3 v| ikke forv¢nter.." to "ENRRROLL3VIKKEFORVNTER"
        )

        rollerOgForventetUtfall.forEach { (rolle, forventetMetricVerdi) ->
            assertEquals(forventetMetricVerdi, rolle.metricsFriendly())
        }
    }
}