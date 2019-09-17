package no.nav.k9.utgaende.ws

import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*
import javax.xml.datatype.DatatypeFactory

private val datatypeFactory = DatatypeFactory.newInstance()

internal fun LocalDate.toXmlGregorianCalendar() = this.let {
    val gcal = GregorianCalendar.from(this.atStartOfDay(ZoneOffset.UTC))
    datatypeFactory.newXMLGregorianCalendar(gcal)
}