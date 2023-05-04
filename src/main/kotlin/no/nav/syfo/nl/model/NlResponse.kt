package no.nav.syfo.nl.model

data class NlResponse(
    val orgnummer: String,
    val utbetalesLonn: Boolean?,
    val leder: Leder,
    val sykmeldt: Sykmeldt,
)
