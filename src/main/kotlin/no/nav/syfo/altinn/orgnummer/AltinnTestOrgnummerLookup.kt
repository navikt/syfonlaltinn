package no.nav.syfo.altinn.orgnummer

class AltinnTestOrgnummerLookup : AltinnOrgnummerLookup {
    private val overrideOrgnummer: String = AltinnTestOrgnummerLookup::class.java.getResource("/altinn/altinn_test_overstyr_orgnr").readText().trim()
    private val altinnWhitelistOrgnummer: List<String> = AltinnTestOrgnummerLookup::class.java.getResource("/altinn/altinn_test_whitelist_orgnr").readText().trim().split(",")

    override fun getOrgnummer(orgnummer: String): String {
        return when (altinnWhitelistOrgnummer.contains(orgnummer)) {
            true -> orgnummer
            else -> overrideOrgnummer
        }
    }
}
