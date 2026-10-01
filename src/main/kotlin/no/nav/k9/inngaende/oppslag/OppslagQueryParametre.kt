package no.nav.k9.inngaende.oppslag

import jakarta.servlet.http.HttpServletRequest
import no.nav.k9.http.ParameterType
import no.nav.k9.http.ValidationErrorResponseException
import no.nav.k9.http.Violation
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZoneId

private const val ATTRIBUTT_QUERY_NAVN = "a"
private const val FRA_OG_MED_QUERY_NAVN = "fom"
private const val TIL_OG_MED_QUERY_NAVN = "tom"
internal const val INKLUDER_ALLE_ANSETTELSESPERIODER = "inkluderAlleAnsettelsesperioder"
private const val ORGANISASJONER = "org"

private val logger = LoggerFactory.getLogger("no.nav.k9.inngaende.oppslag.OppslagQueryParametre")

internal fun HttpServletRequest.hentAttributter(): Set<Attributt> {
    val ikkeStøttedeAttributter = mutableSetOf<Violation>()
    val støttedeAttributter = mutableSetOf<Attributt>()

    val etterspurteAttributter = getParameterValues(ATTRIBUTT_QUERY_NAVN)
        ?.filter { it.isNotBlank() }
        ?.map { it.lowercase() }
        ?.toSet() ?: emptySet()

    logger.info("Etterspurte Attributter = [${etterspurteAttributter.joinToString(", ")}]")

    etterspurteAttributter.forEach {
        try {
            støttedeAttributter.add(Attributt.fraApi(it))
        } catch (cause: Throwable) {
            ikkeStøttedeAttributter.add(
                Violation(
                    parameterType = ParameterType.QUERY,
                    parameterName = ATTRIBUTT_QUERY_NAVN,
                    invalidValue = it,
                    reason = "Er ikke en støttet attributt."
                )
            )
        }
    }

    return if (ikkeStøttedeAttributter.isEmpty()) støttedeAttributter
    else throw ValidationErrorResponseException(ikkeStøttedeAttributter)
}

internal fun HttpServletRequest.hentFraOgMedTilOgMed(): Pair<LocalDate, LocalDate> {
    val fomQuery = getParameter(FRA_OG_MED_QUERY_NAVN)
    val tomQuery = getParameter(TIL_OG_MED_QUERY_NAVN)
    return Pair(
        first = if (fomQuery == null) iDag() else fomQuery.somLocalDate(FRA_OG_MED_QUERY_NAVN),
        second = if (tomQuery == null) iDag() else tomQuery.somLocalDate(TIL_OG_MED_QUERY_NAVN)
    )
}

internal fun HttpServletRequest.hentOrganisasjoner(): Set<String> =
    getParameterValues(ORGANISASJONER)
        ?.filter { it.isNotBlank() }
        ?.map { it.lowercase() }
        ?.toSet() ?: emptySet()

private fun String.somLocalDate(queryParameterName: String) = try {
    LocalDate.parse(this)
} catch (cause: Throwable) {
    throw ValidationErrorResponseException(
        setOf(
            Violation(
                parameterType = ParameterType.QUERY,
                parameterName = queryParameterName,
                invalidValue = this,
                reason = "Må være på format yyyy-mm-dd."
            )
        )
    )
}

private fun iDag(): LocalDate = LocalDate.now(ZoneId.of("Europe/Oslo"))
