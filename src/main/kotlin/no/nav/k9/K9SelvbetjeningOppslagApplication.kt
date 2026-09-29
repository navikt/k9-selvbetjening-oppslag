package no.nav.k9

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.resilience.annotation.EnableResilientMethods

@SpringBootApplication
@EnableResilientMethods
class K9SelvbetjeningOppslagApplication

fun main(args: Array<String>) {
    runApplication<K9SelvbetjeningOppslagApplication>(*args)
}
