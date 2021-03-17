package no.nav.syfo.altinn.narmesteleder.exception

import java.lang.IllegalArgumentException

class ValidationException(val type: String, message: String) : IllegalArgumentException(message)
