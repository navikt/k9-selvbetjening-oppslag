package no.nav.k9.integrasjon.enhetsregister

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.currentCoroutineContext
import no.nav.k9.inngaende.correlationId
import no.nav.k9.inngaende.oppslag.Attributt
import no.nav.k9.integrasjon.common.getStringOrNull
import no.nav.k9.integrasjon.common.logResponse
import no.nav.k9.integrasjon.common.påkrevd
import no.nav.k9.integrasjon.common.restKall
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.time.LocalDate

@Service
internal class EnhetsregisterService(
    private val retryClient: EnhetsregisterRetryClient,
    private val objectMapper: ObjectMapper,
    @Value("\${nav.register-urls.enhetsregister-v1}") private val baseUrl: URI,
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(EnhetsregisterService::class.java)
        private const val NØKKELINFO_PATH = "/organisasjon/{organisasjonsnummer}/noekkelinfo"

        private val støttedeAttributter = setOf(
            Attributt.arbeidsgivereOrganisasjonerNavn
        )
    }

    internal suspend fun enhet(
        organisasjonsnummer: String,
        attributter: Set<Attributt>
    ): Enhet? {
        if (!attributter.any { it in støttedeAttributter }) return null
        return nøkkelinfo(organisasjonsnummer)
    }

    private suspend fun nøkkelinfo(organisasjonsnummer: String): Enhet {
        val callId = currentCoroutineContext().correlationId().value

        logger.restKall("${baseUrl.toString().trimEnd('/')}$NØKKELINFO_PATH")

        val json = objectMapper.readTree(retryClient.nøkkelinfo(NØKKELINFO_PATH, organisasjonsnummer, callId))
        check(json.isObject) { "Forventet et objekt fra enhetsregisteret." }

        logger.logResponse(json)

        if (!json.has("navn")) {
            logger.warn("Ingen navn tilgjenelig for organisasjon ${organisasjonsnummer}. Response = '$json'")
            return Enhet(
                organisasjonsnummer = organisasjonsnummer,
                navn = null,
                enhetstype = json.enhetstype(),
                opphørsdato = json.opphørsdato()
            )
        }
        val navn = json.påkrevd("navn")

        val navnlinjer = listOf(
            navn.navnlinje(1),
            navn.navnlinje(2),
            navn.navnlinje(3),
            navn.navnlinje(4),
            navn.navnlinje(5))
            .filterNot { it.isNullOrBlank() }

        val sammensattNavn = if (navnlinjer.isEmpty()) null else {
            navnlinjer.joinToString(", ")
        }

        return Enhet(
            organisasjonsnummer = organisasjonsnummer,
            navn = sammensattNavn,
            enhetstype = json.enhetstype(),
            opphørsdato = json.opphørsdato()
        )
    }

    private fun JsonNode.navnlinje(nummer: Int) = getStringOrNull("navnelinje$nummer")
    private fun JsonNode.enhetstype() = getStringOrNull("enhetstype")
    private fun JsonNode.opphørsdato() : LocalDate? {
        val stringValue = getStringOrNull("opphoersdato") ?: return null
        return LocalDate.parse(stringValue)
    }
}
