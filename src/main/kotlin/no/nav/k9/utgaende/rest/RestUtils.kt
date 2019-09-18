package no.nav.k9.utgaende.rest

import org.slf4j.Logger

internal fun Logger.restKall(url: String) = info("Utgående kall til $url")