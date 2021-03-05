package no.nav.syfo.nl

import no.nav.syfo.altinn.narmesteleder.NarmesteLederRequestService
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.nl.model.NlRequest
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class NarmesteLederService(
    private val kafkaConsumer: KafkaConsumer<String, NlRequest>,
    private val applicationState: ApplicationState,
    private val topic: String,
    private val narmesteLederService: NarmesteLederRequestService
) {

    fun startConsumer() {
        kafkaConsumer.subscribe(listOf(topic))
        while (applicationState.ready) {
            kafkaConsumer.poll(Duration.ZERO).forEach {
            }
        }
    }
}
