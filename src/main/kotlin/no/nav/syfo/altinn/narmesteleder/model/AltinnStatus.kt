package no.nav.syfo.altinn.narmesteleder.model

import java.time.OffsetDateTime
import java.util.UUID

data class AltinnStatus(
    val id: UUID,
    val sykmeldingId: UUID?,
    val orgNr: String,
    val fnr: String,
    val timestamp: OffsetDateTime,
    val status: Status,
    val sendersReference: String?,
) {
    enum class Status {
        SENDT,
        NEW,
        ERROR,
    }
}
