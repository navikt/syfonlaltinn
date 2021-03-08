package no.nav.syfo

import no.nav.syfo.altinn.narmesteleder.util.fixEmailFormat
import org.amshove.kluent.shouldBeEqualTo
import org.apache.commons.validator.routines.EmailValidator
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class EmailValidatorTest : Spek({
    describe("validate email") {
        it("Should validate") {
            val email = " a.b@a.com"
            val correctEmail = fixEmailFormat(email)
            EmailValidator.getInstance().isValid(email) shouldBeEqualTo false
            EmailValidator.getInstance().isValid(correctEmail) shouldBeEqualTo true
        }
    }
})
