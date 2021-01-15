package no.nav.syfo.nl.model

data class NlRequest(
    val sykmeldingId: String,
    val fnr: String,
    val orgnr: String,
    val name: String
)
