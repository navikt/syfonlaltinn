package no.nav.syfo.altinn.orgnummer

interface AltinnOrgnummerLookup {
    fun getOrgnummer(orgnummer: String): String
}
