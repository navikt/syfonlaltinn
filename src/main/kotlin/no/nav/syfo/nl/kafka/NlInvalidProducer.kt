package no.nav.syfo.nl.kafka

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class NlInvalidProducer(
    private val topic: String,
    private val producer: KafkaProducer<String, Any>
) {
    fun send(key: String, value: Any) {
        producer.send(ProducerRecord(topic, key, value)).get()
    }
}
