package no.nav.syfo.altinn.narmesteleder.model

import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType
import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType.EMAIL
import no.altinn.schemas.serviceengine.formsengine._2009._10.TransportType.SMS
import no.altinn.schemas.services.serviceengine.notification._2009._10.Notification
import no.altinn.schemas.services.serviceengine.notification._2009._10.NotificationBEList
import no.altinn.schemas.services.serviceengine.notification._2009._10.ReceiverEndPoint
import no.altinn.schemas.services.serviceengine.notification._2009._10.ReceiverEndPointBEList
import no.altinn.schemas.services.serviceengine.notification._2009._10.TextToken
import no.altinn.schemas.services.serviceengine.notification._2009._10.TextTokenSubstitutionBEList

class NotificationAltinnGenerator private constructor() {
    companion object {
        private const val NORSK_BOKMAL = "1044"
        private const val FRA_EPOST_ALTINN = "noreply@altinn.no"

        fun createNotifications(): NotificationBEList {
            return NotificationBEList()
                .withNotification(epostNotification(), smsNotification())
        }

        fun createEmailNotification(vararg text: String): Notification {
            return createNotification(FRA_EPOST_ALTINN, EMAIL, convertToTextTokens(*text))
        }

        fun createSmsNotification(vararg text: String): Notification {
            return createNotification(null, SMS, convertToTextTokens(*text))
        }

        private fun epostNotification(): Notification? {
            return createEmailNotification(
                "Meld inn nærmeste leder i Altinn",
                "<p>En ansatt i \$reporteeName$ (\$reporteeNumber$) er sykmeldt og mangler nærmeste leder.</p>" +
                    "<p>Logg inn på Altinn for å melde inn hvem som er nærmeste leder</p>" +
                    "<p>Vennlig hilsen NAV.</p>"
            )
        }

        private fun smsNotification(): Notification? {
            return createSmsNotification(
                "En ansatt i \$reporteeName$ (\$reporteeNumber$) er sykmeldt og mangler nærmeste leder.",
                "Logg inn på Altinn for å melde inn hvem som er nærmeste leder. Vennlig hilsen NAV."
            )
        }

        fun createNotification(fromEmail: String?, type: TransportType, textTokens: Array<TextToken?>): Notification {
            if (textTokens.size != 2) {
                throw IllegalArgumentException("Antall textTokens må være 2. Var ${textTokens.size}")
            }
            return Notification()
                .withLanguageCode(
                    NORSK_BOKMAL
                )
                .withNotificationType(
                    "TokenTextOnly"
                )
                .withFromAddress(
                    fromEmail?.let {
                        it
                    }
                )
                .withReceiverEndPoints(
                    ReceiverEndPointBEList()
                        .withReceiverEndPoint(
                            ReceiverEndPoint()
                                .withTransportType(
                                    type
                                )
                        )
                )
                .withTextTokens(
                    TextTokenSubstitutionBEList().withTextToken(
                        *textTokens
                    )
                )
        }

        private fun convertToTextTokens(vararg text: String): Array<TextToken?> {
            val textTokens = arrayOfNulls<TextToken>(text.size)
            for (i in text.indices) {
                textTokens[i] = TextToken().withTokenNum(i).withTokenValue(
                    text[i]
                )
            }
            return textTokens
        }
    }
}
