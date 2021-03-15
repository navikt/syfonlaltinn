package no.nav.syfo.altinn.narmesteleder.util

import no.nav.syfo.log

public fun fixEmailFormat(nlEpost: String): String {
    var newEmail = nlEpost
    if (newEmail.contains(";")) {
        log.info("Email contains ; $newEmail removes everything after ;")
        newEmail = newEmail.split(";")[0]
    }
    return newEmail.trim()
}
