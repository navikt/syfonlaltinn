package no.nav.syfo.altinn.orgnummer

import io.kotest.core.spec.style.FunSpec
import org.amshove.kluent.shouldBeEqualTo

class AltinnOrgnummerResolverTest : FunSpec({

    val testOverride = "910067494"

    context("Test OrgnummerResolvers") {
        test("Shuild get testOverride") {
            testOverride shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-gcp").getOrgnummer("other")
            testOverride shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-gcp").getOrgnummer("whitelist4")
        }
        test("should get whitelisted") {
            "811290572" shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-gcp").getOrgnummer("811290572")
            "811290742" shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-gcp").getOrgnummer("811290742")
            "910975439" shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-gcp").getOrgnummer("910975439")
        }
        test("Should get same orgnummer in prod") {
            "1" shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("prod-gcp").getOrgnummer("1")
            "2" shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("prod-gcp").getOrgnummer("2")
            "3" shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("prod-gcp").getOrgnummer("3")
        }
        test("Should not send notification if not whitelisted in dev") {
            false shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-gcp").shouldSendNotification("other")
        }
        test("Should send notification if whitelisted in dev") {
            true shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-gcp").shouldSendNotification("811290572")
        }
        test("Should send notificatin in prod") {
            true shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("prod-gcp").shouldSendNotification("any")
        }
    }
})
