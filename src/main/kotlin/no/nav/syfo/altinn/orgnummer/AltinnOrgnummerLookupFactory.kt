package no.nav.syfo.altinn.orgnummer

class AltinnOrgnummerLookupFactory private constructor() {
    companion object {

        private val DEV_CLUSTER = listOf("dev-fss", "dev-gcp")

        fun getOrgnummerResolver(cluster: String): AltinnOrgnummerLookup {
            return when (DEV_CLUSTER.contains(cluster)) {
                true -> AltinnTestOrgnummerLookup()
                else -> AltinnProdOrgnummerLookup()
            }
        }
    }
}
