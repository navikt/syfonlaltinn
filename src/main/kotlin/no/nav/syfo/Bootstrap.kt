package no.nav.syfo

import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasic
import no.altinn.services.serviceengine.prefill._2009._10.IPreFillExternalBasic
import no.nav.syfo.altinn.narmesteleder.NarmesteLederDownloadService
import no.nav.syfo.altinn.narmesteleder.NarmesteLederRequestService
import no.nav.syfo.altinn.orgnummer.AltinnOrgnummerLookupFactory
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.azuread.AccessTokenClient
import no.nav.syfo.db.Database
import no.nav.syfo.httpclient.HttpClientFactory
import no.nav.syfo.kafka.aiven.KafkaUtils
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.kafka.toProducerConfig
import no.nav.syfo.nl.NarmesteLederRequestConsumerService
import no.nav.syfo.nl.kafka.NlInvalidProducer
import no.nav.syfo.nl.kafka.NlResponseProducer
import no.nav.syfo.nl.kafka.model.NlRequestKafkaMessage
import no.nav.syfo.nl.kafka.model.NlResponseKafkaMessage
import no.nav.syfo.nl.kafka.util.JacksonKafkaDeserializer
import no.nav.syfo.nl.kafka.util.JacksonKafkaSerializer
import no.nav.syfo.pdl.client.PdlClient
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.syfonlaltinn")

@DelicateCoroutinesApi
fun main() {
    val env = Environment()
    DefaultExports.initialize()
    val applicationState = ApplicationState()
    val applicationEngine = createApplicationEngine(
        env,
        applicationState
    )
    val applicationServer = ApplicationServer(applicationEngine, applicationState)

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

    val invalidKafkaProducer = KafkaProducer<String, Any>(
        KafkaUtils
            .getAivenKafkaConfig()
            .toProducerConfig("syfonlaltinn-producer", JacksonKafkaSerializer::class, StringSerializer::class)
    )

    val kafkaConsumer = KafkaConsumer(
        KafkaUtils.getAivenKafkaConfig().toConsumerConfig("syfonlaltinn", JacksonKafkaDeserializer::class).also {
            it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "none"
        },
        StringDeserializer(),
        JacksonKafkaDeserializer(NlRequestKafkaMessage::class)
    )
    val httpclient = HttpClientFactory.getHttpClient()
    val pdlClient = PdlClient(
        HttpClientFactory.getHttpClient(), env.pdlGraphqlPath, env.pdlScope,
        AccessTokenClient(
            env.aadAccessTokenUrl, env.clientId, env.clientSecret, httpclient
        )
    )

    val iPreFillExternalBasic = JaxWsProxyFactoryBean().apply {
        address = env.altinnPrefillUrl
        serviceClass = IPreFillExternalBasic::class.java
    }.create(IPreFillExternalBasic::class.java)

    val altinnOrgnummerLookup = AltinnOrgnummerLookupFactory.getOrgnummerResolver(env.cluster)

    val narmesteLederRequestService = NarmesteLederRequestService(
        env.navUsername,
        env.navPassword,
        iPreFillExternalBasic,
        altinnOrgnummerLookup
    )
    val narmesteLederRequestConsumerService = NarmesteLederRequestConsumerService(
        kafkaConsumer,
        applicationState,
        env.nlRequestTopic,
        narmesteLederRequestService,
        database
    )

    val nlResponseKafkaProducer = NlResponseProducer(kafkaProducer, env.nlResponseTopic)
    val nlInvalidProducer = NlInvalidProducer(env.nlInvalidTopic, invalidKafkaProducer)
    val narmesteLederDownloadService = NarmesteLederDownloadService(
        iDownloadQueueExternalBasic,
        env.navUsername,
        env.navPassword,
        applicationState,
        nlResponseKafkaProducer,
        nlInvalidProducer,
        pdlClient,
        env.cluster
    )

    startBackgroundJob(applicationState) {
        narmesteLederDownloadService.start()
    }

    startBackgroundJob(applicationState) {
        narmesteLederRequestConsumerService.startConsumer()
    }

    applicationServer.start()
}

@DelicateCoroutinesApi
fun startBackgroundJob(applicationState: ApplicationState, block: suspend CoroutineScope.() -> Unit) {
    GlobalScope.launch {
        try {
            block()
        } catch (ex: Exception) {
            log.error("Error in background task, restarting application", ex)
            applicationState.alive = false
            applicationState.ready = false
        }
    }
}
