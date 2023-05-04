package no.nav.syfo.nl.kafka

import no.nav.syfo.nl.kafka.model.KafkaMetadata
import no.nav.syfo.nl.kafka.model.NlResponseKafkaMessage
import no.nav.syfo.nl.model.NlResponse
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.OffsetDateTime
import java.time.ZoneOffset

class NlResponseProducer(val kafkaProducer: KafkaProducer<String, NlResponseKafkaMessage>, val topic: String) {
    fun sendNlResponse(nlResponse: NlResponse) {
        val kafkaMessage = NlResponseKafkaMessage(
            kafkaMetadata = KafkaMetadata(OffsetDateTime.now(ZoneOffset.UTC), "syfonlaltinn"),
            nlResponse = nlResponse,
        )
        kafkaProducer.send(ProducerRecord(topic, nlResponse.orgnummer, kafkaMessage)).get()
    }
}
