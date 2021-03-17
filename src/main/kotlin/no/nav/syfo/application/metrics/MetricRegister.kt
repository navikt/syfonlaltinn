package no.nav.syfo.application.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Histogram

const val METRICS_NS = "syfonlaltinn"

val HTTP_HISTOGRAM: Histogram = Histogram.Builder()
    .namespace(METRICS_NS)
    .labelNames("path")
    .name("requests_duration_seconds")
    .help("http requests durations for incoming requests in seconds")
    .register()

val INVALID_NL_SKJEMA: Counter = Counter.build()
    .name(METRICS_NS)
    .labelNames("error_type")
    .name("invalid_nl_schema")
    .help("Counts number of invalid NL schema sendt by arbeidsgiver")
    .register()
