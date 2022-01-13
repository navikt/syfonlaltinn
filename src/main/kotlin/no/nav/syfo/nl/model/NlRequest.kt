package no.nav.syfo.nl.model

import java.util.UUID

data class NlRequest(
    val requestId: UUID,
    val sykmeldingId: String?,
    val fnr: String,
    val orgnr: String,
    val name: String
)
