package no.nav.syfo

import io.kotest.core.spec.style.FunSpec
import no.nav.syfo.altinn.narmesteleder.util.fixEmailFormat
import org.amshove.kluent.shouldBeEqualTo
import org.apache.commons.validator.routines.EmailValidator

class EmailValidatorTest :
    FunSpec({
        context("validate email") {
            test("Should validate") {
                val email = " a.b@a.com"
                val correctEmail = fixEmailFormat(email)
                EmailValidator.getInstance().isValid(email) shouldBeEqualTo false
                EmailValidator.getInstance().isValid(correctEmail) shouldBeEqualTo true
            }
        }
    })
