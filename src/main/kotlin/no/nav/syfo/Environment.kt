package no.nav.syfo

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "syfonlaltinn"),
    val altinnDownloadUrl: String = getEnvVar("ALTINN_DOWNLOAD_QUEUE_URL"),
    val navUsername: String = getEnvVar("NAV_USERNAME"),
    val navPassword: String = getEnvVar("NAV_PASSWORD")
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
