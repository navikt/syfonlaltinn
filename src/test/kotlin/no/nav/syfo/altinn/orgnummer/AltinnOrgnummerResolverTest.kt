package no.nav.syfo.altinn.orgnummer

import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class AltinnOrgnummerResolverTest : Spek({

    val testOverride = "910067494"

    describe("Test OrgnummerResolvers") {
        it("Shuild get testOverride") {
            testOverride shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-fss").getOrgnummer("other")
            testOverride shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-fss").getOrgnummer("whitelist4")
        }
        it("should get whitelisted") {
            "811290572" shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-fss").getOrgnummer("811290572")
            "811290742" shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-fss").getOrgnummer("811290742")
            "910975439" shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-fss").getOrgnummer("910975439")
        }
        it("Should get same orgnummer in prod") {
            "1" shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("prod-fss").getOrgnummer("1")
            "2" shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("prod-fss").getOrgnummer("2")
            "3" shouldBeEqualTo AltinnOrgnummerLookupFactory.getOrgnummerResolver("prod-fss").getOrgnummer("3")
        }
    }
})
