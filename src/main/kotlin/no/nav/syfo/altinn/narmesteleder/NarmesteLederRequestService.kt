package no.nav.syfo.altinn.narmesteleder

import generated.XMLOppgiPersonallederM
import generated.XMLSkjemainnhold
import generated.XMLSykmeldt
import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.schemas.services.serviceengine.prefill._2009._10.PrefillForm
import no.altinn.schemas.services.serviceengine.prefill._2009._10.PrefillFormBEList
import no.altinn.schemas.services.serviceengine.prefill._2009._10.PrefillFormTask
import no.altinn.services.serviceengine.prefill._2009._10.IPreFillExternalBasic
import no.nav.syfo.log
import no.nav.syfo.nl.model.NlRequest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.GregorianCalendar
import java.util.UUID
import javax.xml.bind.JAXBElement
import javax.xml.datatype.DatatypeFactory.newInstance
import javax.xml.datatype.XMLGregorianCalendar
import javax.xml.namespace.QName

class NarmesteLederRequestService(
    private val navUsername: String,
    private val navPassword: String,
    private val iPreFillExternalBasic: IPreFillExternalBasic
) {
    companion object {
        private const val NARMESTE_LEDER_TJENESTEKODE = "4596"
        private const val DATA_FORMAT_VERSION = 42026
        private const val DATA_FORMAT_ID = "5363"
        private const val DATA_FORMAT_PROVIDER = "SERES"
        private const val SYSTEM_USER_CODE = "NAV_DIGISYFO"
    }

    fun sendRequestToAltinn(nlRequest: NlRequest): String {
        try {
            val receipt = iPreFillExternalBasic.submitAndInstantiatePrefilledFormTaskBasic(
                navUsername,
                navPassword,
                UUID.randomUUID().toString(),
                getPrefillFormTask(nlRequest),
                false,
                true,
                null,
                null
            )
            if (receipt.receiptStatusCode != ReceiptStatusEnum.OK) {
                log.error("Could not sendt NlRequest to altinn for sykmelding :${nlRequest.sykmeldingId}")
                throw RuntimeException("Could not send to altinn")
            }
            return receipt.references.reference.stream()
                .filter { "SendersReference" == it.referenceTypeName.value() }
                .map { it.referenceValue }
                .findFirst()
                .orElseThrow { RuntimeException("Could not find SendersReference") }
        } catch (ex: Exception) {
            log.error("Could not send to altinn", ex)
            throw ex
        }
    }

    private fun getPrefillFormTask(nlRequest: NlRequest): PrefillFormTask {
        return PrefillFormTask()
            .withExternalServiceCode(NARMESTE_LEDER_TJENESTEKODE)
            .withExternalServiceEditionCode(1)
            .withExternalShipmentReference(nlRequest.requestId.toString())
            .withPreFillForms(
                PrefillFormBEList()
                    .withPrefillForm(
                        PrefillForm()
                            .withDataFormatID(DATA_FORMAT_ID)
                            .withDataFormatVersion(DATA_FORMAT_VERSION)
                            .withFormDataXML(generateFormData(nlRequest))
                            .withSendersReference(UUID.randomUUID().toString())
                            .withSignedByDefault(false)
                            .withSigningLocked(false)
                    )
            )
            .withReceiversReference(UUID.randomUUID().toString())
            .withReportee(nlRequest.orgnr)
            .withSendersReference(UUID.randomUUID().toString())
            .withServiceOwnerCode(SYSTEM_USER_CODE)
            .withValidFromDate(createXMLDate(ZonedDateTime.now(ZoneOffset.UTC)))
            .withValidToDate(getDueDate())
    }

    private fun getDueDate(): XMLGregorianCalendar {
        return createXMLDate(ZonedDateTime.now(ZoneOffset.UTC).plusDays(7))
    }

    fun createXMLDate(date: ZonedDateTime): XMLGregorianCalendar {
        return newInstance().newXMLGregorianCalendar(GregorianCalendar.from(date))
    }

    private fun generateFormData(nlRequest: NlRequest): String {
        val xmlOppgiPersonalLeder = JAXBElement(
            QName("melding"), XMLOppgiPersonallederM::class.java,
            XMLOppgiPersonallederM()
                .withDataFormatId(DATA_FORMAT_ID)
                .withDataFormatProvider(DATA_FORMAT_PROVIDER)
                .withDataFormatVersion(DATA_FORMAT_VERSION.toString())
                .withSkjemainnhold(
                    XMLSkjemainnhold()
                        .withSykmeldt(
                            JAXBElement(
                                QName("sykmeldt"),
                                XMLSykmeldt::class.java,
                                XMLSykmeldt()
                                    .withSykmeldtFoedselsnummer(nlRequest.fnr)
                                    .withSykmeldtNavn(nlRequest.name)
                            )
                        )
                        .withOrganisasjonsnummer(nlRequest.orgnr)
                )
        )
        return JAXBUtil.marshall(xmlOppgiPersonalLeder)
    }
}
