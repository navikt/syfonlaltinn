package no.nav.syfo.nl.kafka.model

import no.nav.syfo.nl.model.NlRequest

data class NlRequestKafkaMessage(
    val nlRequest: NlRequest,
    val metadata: KafkaMetadata
)
