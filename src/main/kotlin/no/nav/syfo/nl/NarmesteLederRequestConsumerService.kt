package no.nav.syfo.nl

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.delay
import no.nav.syfo.altinn.narmesteleder.NarmesteLederRequestService
import no.nav.syfo.altinn.narmesteleder.db.erSendtSisteUke
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

class NarmesteLederRequestConsumerService(
    private val kafkaConsumer: KafkaConsumer<String, NlRequestKafkaMessage>,
    private val applicationState: ApplicationState,
    private val topic: String,
    private val narmesteLederService: NarmesteLederRequestService,
    private val database: DatabaseInterface,
) {

    suspend fun startConsumer() {
        kafkaConsumer.subscribe(listOf(topic))
        log.info("Starting consuming topic $topic")
        while (applicationState.ready) {
            kafkaConsumer.poll(10_000.seconds.toJavaDuration()).forEach {
                val nlRequest = it.value().nlRequest
                if (erSendtSisteUke(orgnummer = nlRequest.orgnr, fnr = nlRequest.fnr)) {
                    log.info(
                        "Har sendt tilsvarende NL-skjema som requestId ${nlRequest.requestId} de siste 7 dagene, sender ikke på nytt"
                    )
                } else {
                    val status = database.getAltinnStatus(nlRequest.requestId)
                    when (status?.status) {
                        null -> sendToAltinn(nlRequest, insertNewStatus(nlRequest))
                        AltinnStatus.Status.SENDT ->
                            log.info("Message is already sendt to altinn ${status.id} for")
                        AltinnStatus.Status.ERROR -> sendToAltinn(nlRequest, status)
                        AltinnStatus.Status.NEW -> sendToAltinn(nlRequest, status)
                    }
                }
            }
            delay(1L)
        }
    }

    private suspend fun sendToAltinn(nlRequest: NlRequest, altinnStatus: AltinnStatus) {
        try {
            log.info(
                "Sender NL-forespørsel for requestId: ${nlRequest.requestId}, sykmeldingId: ${nlRequest.sykmeldingId}"
            )
            val sendersReference = narmesteLederService.sendRequestToAltinn(nlRequest)
            database.updateAltinnStatus(
                altinnStatus.copy(
                    status = AltinnStatus.Status.SENDT,
                    sendersReference = sendersReference
                )
            )
        } catch (ex: Exception) {
            log.error("Error updating altinn")
            database.updateAltinnStatus(altinnStatus.copy(status = AltinnStatus.Status.ERROR))
            throw ex
        }
    }

    private fun insertNewStatus(nlRequest: NlRequest): AltinnStatus {
        val sykmeldingId =
            nlRequest.sykmeldingId?.let {
                try {
                    UUID.fromString(nlRequest.sykmeldingId)
                } catch (e: Exception) {
                    log.warn(
                        "Sykmeldingid ${nlRequest.sykmeldingId} er ikke uuid, bruker requestId"
                    )
                    nlRequest.requestId
                } ?: null
            }
        val altinnStatus =
            AltinnStatus(
                id = nlRequest.requestId,
                sykmeldingId = sykmeldingId,
                orgNr = nlRequest.orgnr,
                fnr = nlRequest.fnr,
                OffsetDateTime.now(ZoneOffset.UTC),
                AltinnStatus.Status.NEW,
                sendersReference = null,
            )
        database.insertAltinnStatus(altinnStatus)
        return altinnStatus
    }

    private fun erSendtSisteUke(
        orgnummer: String,
        fnr: String,
        enUkeSiden: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC).minusWeeks(1),
    ): Boolean {
        return database.erSendtSisteUke(orgnummer = orgnummer, fnr = fnr, enUkeSiden = enUkeSiden)
    }
}
