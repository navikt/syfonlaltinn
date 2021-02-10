package no.nav.syfo.nl.kafka.model

import no.nav.syfo.nl.model.NlResponse

data class NlResponseKafkaMessage(
    val kafkaMetadata: KafkaMetadata,
    val nlResponse: NlResponse
)
