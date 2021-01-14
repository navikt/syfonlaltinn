package no.nav.syfo.db

import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.Environment
import no.nav.syfo.altinn.narmesteleder.db.getAltinnStatus
import no.nav.syfo.altinn.narmesteleder.db.getAltinnStatusBySykmeldingId
import no.nav.syfo.altinn.narmesteleder.db.insertAltinnStatus
import no.nav.syfo.altinn.narmesteleder.db.updateAltinnStatus
import no.nav.syfo.altinn.narmesteleder.model.AltinnStatus
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import org.testcontainers.containers.PostgreSQLContainer
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertFailsWith

class PsqlContainer : PostgreSQLContainer<PsqlContainer>("postgres:12.0")

class DatabaseTest : Spek({
    val mockEnv = mockk<Environment>(relaxed = true)
    every { mockEnv.databaseUsername } returns "username"
    every { mockEnv.databasePassword } returns "password"

    val psqlContainer = PsqlContainer()
        .withExposedPorts(5432)
        .withUsername("username")
        .withPassword("password")
        .withDatabaseName("databasename1")
        .withInitScript("db/testdb-init.sql")

    psqlContainer.start()

    beforeEachTest {
        every { mockEnv.databaseUsername } returns "username"
        every { mockEnv.databasePassword } returns "password"
    }

    describe("Test database") {
        it("Should fail 20 times then connect") {
            every { mockEnv.jdbcUrl() } returnsMany (0 until 20).map { "jdbc:postgresql://127.0.0.1:5433/databasename1" } andThen psqlContainer.jdbcUrl
            Database(mockEnv, 30, 0)
        }
        it("Fail after timeout exeeded") {
            every { mockEnv.jdbcUrl() } returns "jdbc:postgresql://127.0.0.1:5433/databasename1"
            assertFailsWith<RuntimeException> { Database(mockEnv, retries = 30, sleepTime = 0) }
        }
    }

    describe("Test db queries") {
        every { mockEnv.jdbcUrl() } returns psqlContainer.jdbcUrl
        val database = Database(mockEnv)

        it("Insert new sykmeldingStatus") {
            val altinnStatus = AltinnStatus(
                id = UUID.randomUUID(),
                sykmeldingId = UUID.randomUUID(),
                orgNr = "orgnr",
                fnr = "fnr",
                timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                status = AltinnStatus.Status.PENDING
            )
            database.insertAltinnStatus(altinnStatus)

            val queryById = database.getAltinnStatus(altinnStatus.id)
            queryById shouldBeEqualTo altinnStatus

            val queryBySykmeldingId = database.getAltinnStatusBySykmeldingId(altinnStatus.sykmeldingId)
            queryBySykmeldingId shouldBeEqualTo altinnStatus


        }

        it("Update status") {
            val altinnStatus = AltinnStatus(
                id = UUID.randomUUID(),
                sykmeldingId = UUID.randomUUID(),
                orgNr = "orgnr",
                fnr = "fnr",
                timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                status = AltinnStatus.Status.PENDING
            )
            database.insertAltinnStatus(altinnStatus)

            val updated = altinnStatus.copy(status = AltinnStatus.Status.DONE)
            database.updateAltinnStatus(updated)

            val status = database.getAltinnStatus(altinnStatus.id)
            status!!.status shouldBeEqualTo updated.status
        }

    }
})
