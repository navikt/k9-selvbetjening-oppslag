package no.nav.k9.utgaende.gateway

private object MetricsFriendly {
    internal val METRIC_FRIENDLY = "[^A-Z0-9]".toRegex()
}
internal fun String.metricsFriendly() = this
    .uppercase()
    .replace(" ", "")
    .replace(MetricsFriendly.METRIC_FRIENDLY, "")
