package no.nav.syfo.altinn.narmesteleder

import generated.XMLSkjemainnhold
import kotlinx.coroutines.delay
import no.altinn.schemas.services.archive.downloadqueue._2012._08.DownloadQueueItemBE
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasic
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasicGetDownloadQueueItemsAltinnFaultFaultFaultMessage
import no.nav.syfo.altinn.narmesteleder.JAXBUtil.Companion.unmarshallNarmesteLederSkjema
import no.nav.syfo.altinn.narmesteleder.util.fixEmailFormat
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.log
import no.nav.syfo.nl.kafka.NlResponseProducer
import no.nav.syfo.nl.model.Leder
import no.nav.syfo.nl.model.NlResponse
import no.nav.syfo.nl.model.Sykmeldt
import org.apache.commons.validator.routines.EmailValidator

class NarmesteLederDownloadService(
    private val iDownloadQueueExternalBasic: IDownloadQueueExternalBasic,
    private val navUsername: String,
    private val navPassword: String,
    private val applicationState: ApplicationState,
    private val nlResponseProducer: NlResponseProducer
) {

    companion object {
        private const val SERVICE_CODE = "4596"
        private const val LANGUAGE_ID = 1033
        private const val DELAY = 60_000L * 1
    }

    suspend fun start() {
        while (applicationState.ready) {
            pollDownloadQueueAndHandle()
            delay(DELAY)
        }
    }

    private fun pollDownloadQueueAndHandle() {
        try {
            val items = iDownloadQueueExternalBasic.getDownloadQueueItems(navUsername, navPassword, SERVICE_CODE)
            log.info("Got itmes from download queue from altinn ${items.downloadQueueItemBE.size}")
            items.downloadQueueItemBE.forEach { handleDownloadItem(it) }
        } catch (ex: IDownloadQueueExternalBasicGetDownloadQueueItemsAltinnFaultFaultFaultMessage) {
            log.error("Error getting items from DownloadQueueu" + ex.faultInfo.altinnErrorMessage)
        } catch (ex: Exception) {
            log.error("Error getting download items from altinn", ex)
            throw ex
        }
    }

    private fun handleDownloadItem(it: DownloadQueueItemBE) {
        val item = iDownloadQueueExternalBasic.getArchivedFormTaskBasicDQ(navUsername, navPassword, it.archiveReference, LANGUAGE_ID, true)

        item.forms.archivedFormDQBE.forEach {
            val formData = unmarshallNarmesteLederSkjema(it.formData)
            try {
                val nlResponse = toNlResponse(formData.skjemainnhold)
                nlResponseProducer.sendNlResponse(nlResponse)
                log.info("Got item from altinn download queue ${item.archiveReference} and sendt to kafka")
            } catch (e: IllegalArgumentException) {
                log.error("Kunne ikke behandle NL-skjema ${item.archiveReference}: ${e.message}")
            }
        }
        iDownloadQueueExternalBasic.purgeItem(navUsername, navPassword, it.archiveReference)
        log.info("Deleted ${it.archiveReference} from download queue")
    }
    private fun toNlResponse(skjemaInnhold: XMLSkjemainnhold): NlResponse {
        val orgnummer = skjemaInnhold.organisasjonsnummer
        val utbetalesLonn = skjemaInnhold.utbetalesLonn?.value
        var nlEpost = skjemaInnhold.naermesteLeder.value.naermesteLederEpost.value
        val nlFnr = skjemaInnhold.naermesteLeder.value.naermesteLederFoedselsnummer.value
        val nlMobil = skjemaInnhold.naermesteLeder.value.naermesteLederMobilnummer.value
        val nlFornavn = skjemaInnhold.naermesteLeder.value.naermesteLederFornavn.value
        val nlEtternavn = skjemaInnhold.naermesteLeder.value.naermesteLederEtternavn.value
        val sykmeldtFnr = skjemaInnhold.sykmeldt.value.sykmeldtFoedselsnummer
        val sykmeldtNavn = skjemaInnhold.sykmeldt.value.sykmeldtNavn

        nlEpost = fixEmailFormat(nlEpost)

        validateInputs(nlFnr, nlEpost)

        return NlResponse(
            orgnummer = orgnummer,
            utbetalesLonn = utbetalesLonn,
            leder = Leder(
                fnr = nlFnr,
                mobil = nlMobil,
                epost = nlEpost,
                fornavn = nlFornavn,
                etternavn = nlEtternavn
            ),
            sykmeldt = Sykmeldt(
                fnr = sykmeldtFnr,
                navn = sykmeldtNavn
            )
        )
    }

    private fun validateInputs(nlFnr: String, nlEpost: String) {
        if (!nlFnr.matches(Regex("^\\d{11}\$"))) {
            throw IllegalArgumentException("FNR is not 11 digits")
        }
        val emailValidator = EmailValidator.getInstance()
        if (!emailValidator.isValid(nlEpost)) {
            throw IllegalArgumentException("Email is not valid $nlEpost")
        }
    }
}
