package no.nav.syfo

data class Environment(
    val applicationPort: Int = getEnvVar("APPLICATION_PORT", "8080").toInt(),
    val applicationName: String = getEnvVar("NAIS_APP_NAME", "syfonlaltinn"),
    val altinnDownloadUrl: String = getEnvVar("ALTINN_DOWNLOAD_QUEUE_URL"),
    val altinnPrefillUrl: String = getEnvVar("ALTINN_PREFILL_URL"),
    val navUsername: String = getEnvVar("NAV_USERNAME"),
    val navPassword: String = getEnvVar("NAV_PASSWORD"),
    val databaseUsername: String = getEnvVar("NAIS_DATABASE_SYFONLALTINN_SYFONLALTINN_USERNAME"),
    val databasePassword: String = getEnvVar("NAIS_DATABASE_SYFONLALTINN_SYFONLALTINN_PASSWORD"),
    val dbHost: String = getEnvVar("NAIS_DATABASE_SYFONLALTINN_SYFONLALTINN_HOST"),
    val dbPort: String = getEnvVar("NAIS_DATABASE_SYFONLALTINN_SYFONLALTINN_PORT"),
    val dbName: String = getEnvVar("NAIS_DATABASE_SYFONLALTINN_SYFONLALTINN_DATABASE"),
    val nlResponseTopic: String = "teamsykmelding.syfo-nl-response",
    val nlRequestTopic: String = "teamsykmelding.syfo-nl-request",
    val nlInvalidTopic: String = "teamsykmelding.syfo-nl-invalid",
    val cluster: String = getEnvVar("NAIS_CLUSTER_NAME")
) {
    fun jdbcUrl(): String {
        return "jdbc:postgresql://$dbHost:$dbPort/$dbName"
    }

    companion object {
        fun getEnvVar(varName: String, defaultValue: String? = null) =
            System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")
    }
}
