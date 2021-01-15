package no.nav.syfo.nl

import no.nav.syfo.nl.model.NlRequest
import org.apache.kafka.clients.consumer.KafkaConsumer

class NarmesteLederService(private val kafkaConsumer: KafkaConsumer<String, NlRequest>)
