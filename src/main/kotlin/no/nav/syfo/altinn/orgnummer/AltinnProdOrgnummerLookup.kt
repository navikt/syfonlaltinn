package no.nav.syfo.altinn.orgnummer

class AltinnProdOrgnummerLookup : AltinnOrgnummerLookup {
    override fun getOrgnummer(orgnummer: String): String {
        return orgnummer
    }

    override fun shouldSendNotification(orgnummer: String): Boolean {
        return true
    }
}
