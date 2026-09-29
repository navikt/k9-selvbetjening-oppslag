package no.nav.k9.inngaende

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.boot.availability.ApplicationAvailability
import org.springframework.boot.availability.ReadinessState
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Unprotected
@RestController
internal class ProbeController(
    private val applicationAvailability: ApplicationAvailability,
) {

    @GetMapping("/isalive", produces = ["text/plain;charset=UTF-8"])
    fun isAlive() = "ALIVE"

    // Under graceful shutdown svarer Spring REFUSING_TRAFFIC, og da skal ikke poden ha mer trafikk.
    @GetMapping("/isready", produces = ["text/plain;charset=UTF-8"])
    fun isReady(): ResponseEntity<String> = when (applicationAvailability.readinessState) {
        ReadinessState.ACCEPTING_TRAFFIC -> ResponseEntity.ok("READY")
        else -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("NOT READY")
    }
}
