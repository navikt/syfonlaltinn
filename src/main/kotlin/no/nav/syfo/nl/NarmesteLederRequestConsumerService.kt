package no.nav.syfo.nl

import kotlinx.coroutines.delay
import no.nav.syfo.altinn.narmesteleder.NarmesteLederRequestService
import no.nav.syfo.altinn.narmesteleder.db.getAltinnStatus
import no.nav.syfo.altinn.narmesteleder.db.insertAltinnStatus
import no.nav.syfo.altinn.narmesteleder.db.updateAltinnStatus
import no.nav.syfo.altinn.narmesteleder.model.AltinnStatus
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.log
import no.nav.syfo.nl.kafka.model.NlRequestKafkaMessage
import no.nav.syfo.nl.model.NlRequest
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

class NarmesteLederRequestConsumerService(
    private val kafkaConsumer: KafkaConsumer<String, NlRequestKafkaMessage>,
    private val applicationState: ApplicationState,
    private val topic: String,
    private val narmesteLederService: NarmesteLederRequestService,
    private val database: DatabaseInterface
) {

    suspend fun startConsumer() {
        kafkaConsumer.subscribe(listOf(topic))
        while (applicationState.ready) {
            kafkaConsumer.poll(Duration.ZERO).forEach {
                val status = database.getAltinnStatus(it.value().nlRequest.requestId)
                when (status?.status) {
                    null -> sendToAltinn(it.value().nlRequest, insertNewStatus(it.value().nlRequest))
                    AltinnStatus.Status.SENDT -> log.info("Message is already sendt to altinn ${status.id} for")
                    AltinnStatus.Status.ERROR -> sendToAltinn(it.value().nlRequest, status)
                    AltinnStatus.Status.NEW -> sendToAltinn(it.value().nlRequest, status)
                }
            }
            delay(1L)
        }
    }

    private fun sendToAltinn(nlRequest: NlRequest, altinnStatus: AltinnStatus) {
        try {
            val sendersReference = narmesteLederService.sendRequestToAltinn(nlRequest)
            database.updateAltinnStatus(altinnStatus.copy(status = AltinnStatus.Status.SENDT, sendersReference = sendersReference))
        } catch (ex: Exception) {
            log.error("Error updating altinn")
            database.updateAltinnStatus(altinnStatus.copy(status = AltinnStatus.Status.ERROR))
            throw ex
        }
    }

    private fun insertNewStatus(nlRequest: NlRequest): AltinnStatus {
        val altinnStatus = AltinnStatus(
            id = nlRequest.requestId,
            sykmeldingId = nlRequest.sykmeldingId,
            orgNr = nlRequest.orgnr,
            fnr = nlRequest.fnr,
            OffsetDateTime.now(ZoneOffset.UTC),
            AltinnStatus.Status.NEW,
            sendersReference = null
        )
        database.insertAltinnStatus(altinnStatus)
        return altinnStatus
    }
}
