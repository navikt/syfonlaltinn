package no.nav.syfo

import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasic
import no.nav.syfo.altinn.narmesteleder.NarmesteLederDownloadService
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
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

    val iDownloadQueueExternalBasic = JaxWsProxyFactoryBean().apply {
        address = env.altinnDownloadUrl
        serviceClass = IDownloadQueueExternalBasic::class.java
    }.create(IDownloadQueueExternalBasic::class.java)

    val narmesteLederDownloadService = NarmesteLederDownloadService(iDownloadQueueExternalBasic, env.navUsername, env.navPassword, applicationState)

    GlobalScope.launch {
        narmesteLederDownloadService.start()
    }
}
