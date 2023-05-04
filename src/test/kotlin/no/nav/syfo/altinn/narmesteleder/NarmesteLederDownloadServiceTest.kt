package no.nav.syfo.altinn.narmesteleder

import generated.XMLNaermesteLeder
import generated.XMLOppgiPersonallederM
import generated.XMLSkjemainnhold
import generated.XMLSykmeldt
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.delay
import no.altinn.schemas.services.archive.downloadqueue._2012._08.DownloadQueueItemBE
import no.altinn.schemas.services.archive.downloadqueue._2012._08.DownloadQueueItemBEList
import no.altinn.schemas.services.archive.reporteearchive._2012._08.ArchivedFormDQBE
import no.altinn.schemas.services.archive.reporteearchive._2012._08.ArchivedFormListDQBE
import no.altinn.schemas.services.archive.reporteearchive._2012._08.ArchivedFormTaskDQBE
import no.altinn.services.archive.downloadqueue._2012._08.IDownloadQueueExternalBasic
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.nl.kafka.NlInvalidProducer
import no.nav.syfo.nl.kafka.NlResponseProducer
import no.nav.syfo.pdl.client.PdlClient
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName

class NarmesteLederDownloadServiceTest : FunSpec({

    val iDownload = mockk<IDownloadQueueExternalBasic>()
    val applicationState = mockk<ApplicationState>()
    val nlResponseProducer = mockk<NlResponseProducer>()
    val nlInvalidProducer = mockk<NlInvalidProducer>()
    val pdlClient = mockk<PdlClient>(relaxed = true)
    val service = NarmesteLederDownloadService(
        iDownload,
        "NAV",
        "PASSWORD",
        applicationState,
        nlResponseProducer,
        nlInvalidProducer,
        pdlClient,
        "prod-gcp",
    )
    mockkStatic("kotlinx.coroutines.DelayKt")

    beforeTest {
        clearAllMocks()
        coEvery { delay(any<Long>()) } returns Unit
    }

    context("Test NarmesteLederDownloadService") {
        test("Should handele it") {
            setupTest(iDownload, applicationState, nlResponseProducer, nlInvalidProducer)

            service.start()

            verify(exactly = 1) { iDownload.purgeItem("NAV", "PASSWORD", "1") }
            verify(exactly = 1) { iDownload.getArchivedFormTaskBasicDQ("NAV", "PASSWORD", "1", 1033, true) }
            verify(exactly = 1) { nlResponseProducer.sendNlResponse(any()) }
            verify(exactly = 0) { nlInvalidProducer.send(any(), any()) }
            coVerify(exactly = 2) { pdlClient.getGjeldendeFnr(any()) }
        }

        test("Should not handle incorrect fnr") {
            setupTest(iDownload, applicationState, nlResponseProducer, nlInvalidProducer, "89000000019")

            service.start()

            verify(exactly = 1) { iDownload.purgeItem("NAV", "PASSWORD", "1") }
            verify(exactly = 1) { iDownload.getArchivedFormTaskBasicDQ("NAV", "PASSWORD", "1", 1033, true) }
            verify(exactly = 0) { nlResponseProducer.sendNlResponse(any()) }
            verify(exactly = 1) { nlInvalidProducer.send(any(), any()) }
        }
        test("Should not handle incorrect email") {
            setupTest(iDownload, applicationState, nlResponseProducer, nlInvalidProducer, email = "a.b.c.d")

            service.start()

            verify(exactly = 1) { iDownload.purgeItem("NAV", "PASSWORD", "1") }
            verify(exactly = 1) { iDownload.getArchivedFormTaskBasicDQ("NAV", "PASSWORD", "1", 1033, true) }
            verify(exactly = 0) { nlResponseProducer.sendNlResponse(any()) }
            verify(exactly = 1) { nlInvalidProducer.send(any(), any()) }
        }
    }
})

private fun setupTest(
    iDownload: IDownloadQueueExternalBasic,
    applicationState: ApplicationState,
    nlResponseProducer: NlResponseProducer,
    nlInvalidProducer: NlInvalidProducer,
    lederFnr: String = "14077700162",
    email: String = "epost@epost.com",
) {
    every {
        iDownload.getDownloadQueueItems(
            "NAV",
            "PASSWORD",
            "4596",
        )
    } returns DownloadQueueItemBEList().withDownloadQueueItemBE(
        DownloadQueueItemBE().withArchiveReference("1"),
    )
    every {
        iDownload.getArchivedFormTaskBasicDQ(
            "NAV",
            "PASSWORD",
            "1",
            1033,
            true,
        )
    } returns ArchivedFormTaskDQBE().withForms(
        ArchivedFormListDQBE().withArchivedFormDQBE(
            ArchivedFormDQBE().withFormData(generateFormData(lederFnr, email)),
        ),
    ).withArchiveReference("1")
    every { applicationState.ready } returns true andThen false
    every { iDownload.purgeItem("NAV", "PASSWORD", "1") } returns "1"
    every { nlResponseProducer.sendNlResponse(any()) } returns Unit
    every { nlInvalidProducer.send(any(), any()) } returns Unit
}

private fun generateFormData(lederFnr: String, email: String): String {
    val xmlOppgiPersonalLeder = JAXBElement(
        QName("melding"),
        XMLOppgiPersonallederM::class.java,
        XMLOppgiPersonallederM()
            .withDataFormatId("5363")
            .withDataFormatProvider("SERES")
            .withDataFormatVersion("42026")
            .withSkjemainnhold(
                XMLSkjemainnhold()
                    .withSykmeldt(
                        JAXBElement(
                            QName("sykmeldt"),
                            XMLSykmeldt::class.java,
                            XMLSykmeldt()
                                .withSykmeldtFoedselsnummer("123456789")
                                .withSykmeldtNavn("Navn Navn"),
                        ),
                    )
                    .withNaermesteLeder(
                        JAXBElement(
                            QName("naermesteLeder"),
                            XMLNaermesteLeder::class.java,
                            XMLNaermesteLeder()
                                .withNaermesteLederEpost(
                                    JAXBElement(
                                        QName("naermesteLederEpost"),
                                        String::class.java,
                                        email,
                                    ),
                                )
                                .withNaermesteLederEtternavn(
                                    JAXBElement(
                                        QName("naermesteLederEtternavn"),
                                        String::class.java,
                                        "N",
                                    ),
                                )
                                .withNaermesteLederFornavn(
                                    JAXBElement(
                                        QName("naermesteLederFornavn"),
                                        String::class.java,
                                        "N",
                                    ),
                                )
                                .withNaermesteLederMobilnummer(
                                    JAXBElement(
                                        QName("naermesteLederMobilnummer"),
                                        String::class.java,
                                        "12345678",
                                    ),
                                )
                                .withNaermesteLederFoedselsnummer(
                                    JAXBElement(
                                        QName("naermesteLederFoedselsnummer"),
                                        String::class.java,
                                        lederFnr,
                                    ),
                                ),
                        ),
                    ).withUtbetalesLonn(JAXBElement(QName("utbetalesLonn"), Boolean::class.java, true))
                    .withOrganisasjonsnummer("123456789"),
            ),
    )
    return JAXBUtil.marshall(xmlOppgiPersonalLeder)
}
