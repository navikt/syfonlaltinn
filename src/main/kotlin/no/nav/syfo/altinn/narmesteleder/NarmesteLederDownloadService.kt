package no.nav.syfo.altinn.narmesteleder

import generated.XMLSkjemainnhold
import java.io.IOException
import kotlinx.coroutines.delay
import no.altinn.schemas.services.archive.downloadqueue._2012._08.DownloadQueueItemBE
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasic
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasicGetArchivedFormTaskBasicDQAltinnFaultFaultFaultMessage
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasicGetDownloadQueueItemsAltinnFaultFaultFaultMessage
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasicPurgeItemAltinnFaultFaultFaultMessage
import no.nav.syfo.altinn.narmesteleder.JAXBUtil.Companion.unmarshallNarmesteLederSkjema
import no.nav.syfo.altinn.narmesteleder.exception.ValidationException
import no.nav.syfo.altinn.narmesteleder.util.fixEmailFormat
import no.nav.syfo.altinn.narmesteleder.util.validatePersonAndDNumber
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.metrics.INVALID_NL_SKJEMA
import no.nav.syfo.helpers.retry
import no.nav.syfo.log
import no.nav.syfo.nl.kafka.NlInvalidProducer
import no.nav.syfo.nl.kafka.NlResponseProducer
import no.nav.syfo.nl.model.Leder
import no.nav.syfo.nl.model.NlResponse
import no.nav.syfo.nl.model.Sykmeldt
import no.nav.syfo.pdl.client.PdlClient
import no.nav.syfo.pdl.client.exception.PersonNotFoundException
import no.nav.syfo.securelog
import org.apache.commons.validator.routines.EmailValidator

class NarmesteLederDownloadService(
    private val iDownloadQueueExternalBasic: IDownloadQueueExternalBasic,
    private val navUsername: String,
    private val navPassword: String,
    private val applicationState: ApplicationState,
    private val nlResponseProducer: NlResponseProducer,
    private val nlInvalidProducer: NlInvalidProducer,
    private val pdlClient: PdlClient,
    private val cluster: String,
) {

    companion object {
        private const val SERVICE_CODE = "4596"
        private const val LANGUAGE_ID = 1033
        private const val DELAY = 60_000L
    }

    suspend fun start() {
        while (applicationState.ready) {
            pollDownloadQueueAndHandle()
            delay(DELAY)
        }
    }

    private suspend fun pollDownloadQueueAndHandle() {
        try {
            val items =
                retry(
                    callName = "getDownloadQueueItems",
                    retryIntervals = arrayOf(500L, 1000L, 300L),
                    legalExceptions =
                        arrayOf(
                            IOException::class,
                            IDownloadQueueExternalBasicGetDownloadQueueItemsAltinnFaultFaultFaultMessage::class,
                        ),
                ) {
                    iDownloadQueueExternalBasic.getDownloadQueueItems(
                        navUsername,
                        navPassword,
                        SERVICE_CODE,
                    )
                }
            if (items.downloadQueueItemBE.size > 0) {
                log.info(
                    "Got items from download queue from altinn ${items.downloadQueueItemBE.size}",
                )
            }
            items.downloadQueueItemBE.forEach { handleDownloadItem(it) }
        } catch (ex: IDownloadQueueExternalBasicGetDownloadQueueItemsAltinnFaultFaultFaultMessage) {
            log.error("Error getting items from DownloadQueue" + ex.faultInfo.altinnErrorMessage)
        } catch (ex: IDownloadQueueExternalBasicPurgeItemAltinnFaultFaultFaultMessage) {
            log.error("Error deleting item from DownloadQueue" + ex.faultInfo.altinnErrorMessage)
            throw ex
        } catch (ex: Exception) {
            log.error("Error getting download items from altinn", ex)
            throw ex
        }
    }

    private suspend fun handleDownloadItem(it: DownloadQueueItemBE) {
        val item =
            retry(
                callName = "getArchivedFormTaskBasicDQ",
                retryIntervals = arrayOf(500L, 1000L, 300L),
                legalExceptions =
                    arrayOf(
                        IOException::class,
                        IDownloadQueueExternalBasicGetArchivedFormTaskBasicDQAltinnFaultFaultFaultMessage::class,
                    ),
            ) {
                iDownloadQueueExternalBasic.getArchivedFormTaskBasicDQ(
                    navUsername,
                    navPassword,
                    it.archiveReference,
                    LANGUAGE_ID,
                    true,
                )
            }

        item.forms.archivedFormDQBE.forEach {
            val formData = unmarshallNarmesteLederSkjema(it.formData)
            try {
                securelog.info(
                    "Received NL-skjema, hendelsesId: ${formData.skjemainnhold.hendelseId}, lederFnr: ${formData.skjemainnhold.naermesteLeder.value.naermesteLederFoedselsnummer}, data: ${it.formData}",
                )
                val nlResponse = toNlResponse(formData.skjemainnhold)
                nlResponseProducer.sendNlResponse(nlResponse)
                log.info(
                    "Got item from altinn download queue ${item.archiveReference} and sendt to kafka",
                )
            } catch (e: ValidationException) {
                INVALID_NL_SKJEMA.labels(e.type).inc()
                nlInvalidProducer.send(formData.skjemainnhold.organisasjonsnummer, it)
                log.warn(
                    "Kunne ikke behandle NL-skjema ${item.archiveReference} for orgnummer ${formData.skjemainnhold.organisasjonsnummer}: ${e.message}",
                )
            } catch (e: PersonNotFoundException) {
                if (cluster == "dev-gcp") {
                    log.error("Ignorerer testperson som ikke finnes i PDL i dev")
                } else {
                    securelog.error(
                        "Person not found in PDL. nlFnr: ${formData.skjemainnhold.naermesteLeder.value.naermesteLederFoedselsnummer.value}, " +
                            "sykmeldtFnr: ${formData.skjemainnhold.sykmeldt.value.sykmeldtFoedselsnummer}, " +
                            "archiveRef: ${item.archiveReference}",
                    )
                    log.warn("Skipping NL-skjema ${item.archiveReference} - person not found in PDL")
                }
            }
        }
        retry(
            callName = "getArchivedFormTaskBasicDQ",
            retryIntervals = arrayOf(500L, 1000L, 300L),
            legalExceptions =
                arrayOf(
                    IOException::class,
                    IDownloadQueueExternalBasicPurgeItemAltinnFaultFaultFaultMessage::class,
                ),
        ) {
            iDownloadQueueExternalBasic.purgeItem(navUsername, navPassword, it.archiveReference)
        }
        log.info("Deleted ${it.archiveReference} from download queue")
    }

    private suspend fun toNlResponse(skjemaInnhold: XMLSkjemainnhold): NlResponse {
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

        val pdlNlFnr = pdlClient.getGjeldendeFnr(nlFnr)
        val sykmeldtPdlFnr = pdlClient.getGjeldendeFnr(sykmeldtFnr)

        return NlResponse(
            orgnummer = orgnummer,
            utbetalesLonn = utbetalesLonn,
            leder =
                Leder(
                    fnr = pdlNlFnr,
                    mobil = nlMobil,
                    epost = nlEpost,
                    fornavn = nlFornavn,
                    etternavn = nlEtternavn,
                ),
            sykmeldt =
                Sykmeldt(
                    fnr = sykmeldtPdlFnr,
                    navn = sykmeldtNavn,
                ),
        )
    }

    private fun validateInputs(nlFnr: String, nlEpost: String) {
        if (!validatePersonAndDNumber(nlFnr)) {
            throw ValidationException("INVALID_FNR", "FNR is not valid")
        }
        val emailValidator = EmailValidator.getInstance()
        if (!emailValidator.isValid(nlEpost)) {
            throw ValidationException("INVALID_EMAIL", "Email is not valid")
        }
    }
}
