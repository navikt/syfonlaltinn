package no.nav.syfo

import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasic
import no.nav.syfo.altinn.narmesteleder.NarmesteLederDownloadService
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.db.Database
import no.nav.syfo.kafka.aiven.KafkaUtils
import no.nav.syfo.kafka.toProducerConfig
import no.nav.syfo.nl.kafka.NlResponseProducer
import no.nav.syfo.nl.kafka.model.NlResponseKafkaMessage
import no.nav.syfo.nl.kafka.util.JacksonKafkaSerializer
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfonlaltinn")

fun main() {
    val env = Environment()
    DefaultExports.initialize()
    val applicationState = ApplicationState()
    val applicationEngine = createApplicationEngine(
        env,
        applicationState
    )
    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()
    applicationState.ready = true

    val database = Database(env)
    val iDownloadQueueExternalBasic = JaxWsProxyFactoryBean().apply {
        address = env.altinnDownloadUrl
        serviceClass = IDownloadQueueExternalBasic::class.java
    }.create(IDownloadQueueExternalBasic::class.java)

    val kafkaProducer = KafkaProducer<String, NlResponseKafkaMessage>(
        KafkaUtils
            .getAivenKafkaConfig()
            .toProducerConfig("syfonlaltinn-producer", JacksonKafkaSerializer::class, StringSerializer::class)
    )
    val NlResponseKafkaProducer = NlResponseProducer(kafkaProducer, env.nlResponseTopic)
    val narmesteLederDownloadService = NarmesteLederDownloadService(iDownloadQueueExternalBasic, env.navUsername, env.navPassword, applicationState, NlResponseKafkaProducer)

    GlobalScope.launch {
        narmesteLederDownloadService.start()
    }
}
