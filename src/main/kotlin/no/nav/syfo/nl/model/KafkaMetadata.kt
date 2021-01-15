package no.nav.syfo.nl.model

import java.time.OffsetDateTime

class KafkaMetadata(val timestamp: OffsetDateTime, val source: String, val sykmeldingId: String)
