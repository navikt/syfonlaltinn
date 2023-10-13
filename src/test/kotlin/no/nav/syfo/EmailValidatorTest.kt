package no.nav.syfo

import no.nav.syfo.altinn.narmesteleder.util.fixEmailFormat
import org.amshove.kluent.shouldBeEqualTo
import org.apache.commons.validator.routines.EmailValidator
import org.junit.jupiter.api.Test

class EmailValidatorTest {
    @Test
    internal fun `validate email should validate`() {
        val email = " a.b@a.com"
        val correctEmail = fixEmailFormat(email)
        EmailValidator.getInstance().isValid(email) shouldBeEqualTo false
        EmailValidator.getInstance().isValid(correctEmail) shouldBeEqualTo true
    }
}
