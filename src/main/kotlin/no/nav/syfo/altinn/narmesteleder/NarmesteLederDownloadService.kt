package no.nav.syfo.altinn.narmesteleder

import kotlinx.coroutines.delay
import no.altinn.schemas.services.archive.downloadqueue._2012._08.DownloadQueueItemBE
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasic
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.log

class NarmesteLederDownloadService(
    private val iDownloadQueueExternalBasic: IDownloadQueueExternalBasic,
    private val navUsername: String,
    private val navPassword: String,
    private val applicationState: ApplicationState
) {

    companion object {
        private const val SERVICE_CODE = "4596"
        private const val LANGUAGE_ID = 1033
        private const val DELAY = 60_000L * 5
    }

    suspend fun start() {
        while (applicationState.ready) {
            pollDownloadQueueAndHandle()
            delay(DELAY)
        }
    }

    fun pollDownloadQueueAndHandle() {
        try {
            val items = iDownloadQueueExternalBasic.getDownloadQueueItems(navUsername, navPassword, SERVICE_CODE)
            log.info("Got itmes from download queue from altinn ${items.downloadQueueItemBE.size}")
            items.downloadQueueItemBE.forEach { handleDownloadItem(it) }
        } catch (ex: Exception) {
            log.error("Error getting download items from altinn", ex)
            throw ex
        }
    }

    private fun handleDownloadItem(it: DownloadQueueItemBE) {
        val item = iDownloadQueueExternalBasic.getArchivedFormTaskBasicDQ(navUsername, navPassword, it.archiveReference, LANGUAGE_ID, true)
        val form = item.forms.archivedFormDQBE[0]
        log.info("Got item from altinn download queue ${item.archiveReference}, with form reference ${form.reference} with parent reference ${form.parentReference}")
    }
}
