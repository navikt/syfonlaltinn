package no.nav.syfo.altinn.orgnummer

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

internal class AltinnOrgnummerResolverTest {
    private val testOverride = "910067494"

    @Test
    internal fun `Should get testOverride`() {
        testOverride shouldBeEqualTo
            AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-gcp").getOrgnummer("other")
        testOverride shouldBeEqualTo
            AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-gcp").getOrgnummer("whitelist4")
    }

    @Test
    internal fun `Should get whitelisted`() {
        "811290572" shouldBeEqualTo
            AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-gcp").getOrgnummer("811290572")
        "811290742" shouldBeEqualTo
            AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-gcp").getOrgnummer("811290742")
        "910975439" shouldBeEqualTo
            AltinnOrgnummerLookupFactory.getOrgnummerResolver("dev-gcp").getOrgnummer("910975439")
    }

    @Test
    internal fun `Should get same orgnummer in prod`() {
        "1" shouldBeEqualTo
            AltinnOrgnummerLookupFactory.getOrgnummerResolver("prod-gcp").getOrgnummer("1")
        "2" shouldBeEqualTo
            AltinnOrgnummerLookupFactory.getOrgnummerResolver("prod-gcp").getOrgnummer("2")
        "3" shouldBeEqualTo
            AltinnOrgnummerLookupFactory.getOrgnummerResolver("prod-gcp").getOrgnummer("3")
    }
}
