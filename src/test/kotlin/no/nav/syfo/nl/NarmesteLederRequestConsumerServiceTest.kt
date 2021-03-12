package no.nav.syfo.nl

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.altinn.narmesteleder.NarmesteLederRequestService
import no.nav.syfo.altinn.narmesteleder.db.getAltinnStatus
import no.nav.syfo.altinn.narmesteleder.db.insertAltinnStatus
import no.nav.syfo.altinn.narmesteleder.db.updateAltinnStatus
import no.nav.syfo.altinn.narmesteleder.model.AltinnStatus
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.db.DatabaseInterface
import no.nav.syfo.nl.kafka.model.KafkaMetadata
import no.nav.syfo.nl.kafka.model.NlRequestKafkaMessage
import no.nav.syfo.nl.model.NlRequest
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertFails

class NarmesteLederRequestConsumerServiceTest : Spek({

    val kafkaConsumer = mockk<KafkaConsumer<String, NlRequestKafkaMessage>>()
    val applicationState = mockk<ApplicationState>()
    val narmesteLederRequestService = mockk<NarmesteLederRequestService>()
    val database = mockk<DatabaseInterface>()
    mockkStatic("no.nav.syfo.altinn.narmesteleder.db.DatabaseQueriesKt")

    val service = NarmesteLederRequestConsumerService(kafkaConsumer, applicationState, "topic", narmesteLederRequestService, database)
    val crs = createConsumerRecord()
    beforeEachTest {
        clearAllMocks()
        every { database.insertAltinnStatus(any()) } returns Unit
        every { database.updateAltinnStatus(any()) } returns Unit
        every { applicationState.ready } returns true andThen false
        every { kafkaConsumer.poll(any<Duration>()) } returns crs
        every { kafkaConsumer.subscribe(any<List<String>>()) } returns Unit
        every { database.getAltinnStatus(any()) } returns null
    }

    describe("Test service") {
        it("should send to altinn") {
            every { narmesteLederRequestService.sendRequestToAltinn(any()) } returns "senders_reference"

            runBlocking {
                service.startConsumer()
            }

            val cr = crs.first()
            verify(exactly = 1) { narmesteLederRequestService.sendRequestToAltinn(cr.value().nlRequest) }
            verify(exactly = 1) {
                database.insertAltinnStatus(
                    match {
                        createAltinnStatus(cr.value().nlRequest, it.timestamp) == it
                    }
                )
            }
            verify(exactly = 1) {
                database.updateAltinnStatus(
                    match {
                        it.status == AltinnStatus.Status.SENDT
                        it.sendersReference == "senders_reference"
                    }
                )
            }
        }

        it("Should update db with error when fails to send to altinn") {
            every { narmesteLederRequestService.sendRequestToAltinn(any()) } throws RuntimeException("ERROR FROM ALTINN")
            assertFails {
                runBlocking {
                    service.startConsumer()
                }
            }
            val cr = crs.first()
            verify(exactly = 1) { narmesteLederRequestService.sendRequestToAltinn(cr.value().nlRequest) }
            verify(exactly = 1) {
                database.updateAltinnStatus(
                    match {
                        it.status == AltinnStatus.Status.ERROR &&
                            it.sendersReference == null
                    }
                )
            }
            verify(exactly = 1) {
                database.insertAltinnStatus(
                    match {
                        createAltinnStatus(cr.value().nlRequest, it.timestamp) == it
                    }
                )
            }
        }

        it("Should not send already sent message") {
            every { database.getAltinnStatus(any()) } returns createAltinnStatus(crs.first().value().nlRequest, OffsetDateTime.now(ZoneOffset.UTC)).copy(status = AltinnStatus.Status.SENDT)
            runBlocking {
                service.startConsumer()
            }
            verify(exactly = 0) { database.updateAltinnStatus(any()) }
            verify(exactly = 0) { database.insertAltinnStatus(any()) }
            verify(exactly = 0) { narmesteLederRequestService.sendRequestToAltinn(any()) }

        }

        it("Should retry errors") {
            every { database.getAltinnStatus(any()) } returns createAltinnStatus(crs.first().value().nlRequest, OffsetDateTime.now(ZoneOffset.UTC)).copy(status = AltinnStatus.Status.ERROR)
            every { narmesteLederRequestService.sendRequestToAltinn(any()) } returns "senders_reference"
            runBlocking {
                service.startConsumer()
            }
            verify(exactly = 1) { database.updateAltinnStatus(match {
                it.status == AltinnStatus.Status.SENDT &&
                        it.sendersReference == "senders_reference"
            }) }
            verify(exactly = 0) { database.insertAltinnStatus(any()) }
            verify(exactly = 1) { narmesteLederRequestService.sendRequestToAltinn(any()) }
        }

        it("Should retry when status = NEW") {
            every { database.getAltinnStatus(any()) } returns createAltinnStatus(crs.first().value().nlRequest, OffsetDateTime.now(ZoneOffset.UTC)).copy(status = AltinnStatus.Status.NEW)
            every { narmesteLederRequestService.sendRequestToAltinn(any()) } returns "senders_reference"
            runBlocking {
                service.startConsumer()
            }
            verify(exactly = 1) { database.updateAltinnStatus(match {
                it.status == AltinnStatus.Status.SENDT &&
                        it.sendersReference == "senders_reference"
            }) }
            verify(exactly = 0) { database.insertAltinnStatus(any()) }
            verify(exactly = 1) { narmesteLederRequestService.sendRequestToAltinn(any()) }
        }
    }
})

fun createAltinnStatus(nlRequest: NlRequest, timestamp: OffsetDateTime): AltinnStatus {
    return AltinnStatus(
        id = nlRequest.requestId,
        sykmeldingId = nlRequest.sykmeldingId,
        orgNr = nlRequest.orgnr,
        fnr = nlRequest.fnr,
        timestamp = timestamp,
        status = AltinnStatus.Status.NEW,
        sendersReference = null
    )
}

fun createConsumerRecord(): ConsumerRecords<String, NlRequestKafkaMessage> {

    return ConsumerRecords(
        mapOf(TopicPartition("topic", 1) to listOf<ConsumerRecord<String, NlRequestKafkaMessage>>(ConsumerRecord("topic", 1, 0, "key", getNlRequestMessage())))
    )
}

fun getNlRequestMessage(): NlRequestKafkaMessage {
    return NlRequestKafkaMessage(
        metadata = KafkaMetadata(timestamp = OffsetDateTime.now(ZoneOffset.UTC), "syfoservice"),
        nlRequest = NlRequest(
            requestId = UUID.randomUUID(),
            sykmeldingId = UUID.randomUUID(),
            fnr = "12345678912",
            orgnr = "123456789",
            name = "Syk syk"
        )
    )
}
