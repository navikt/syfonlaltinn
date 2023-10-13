package no.nav.syfo.db

import io.mockk.every
import io.mockk.mockk
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertFailsWith
import no.nav.syfo.Environment
import no.nav.syfo.altinn.narmesteleder.db.erSendtSisteUke
import no.nav.syfo.altinn.narmesteleder.db.getAltinnStatus
import no.nav.syfo.altinn.narmesteleder.db.insertAltinnStatus
import no.nav.syfo.altinn.narmesteleder.db.updateAltinnStatus
import no.nav.syfo.altinn.narmesteleder.model.AltinnStatus
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer

class PsqlContainer : PostgreSQLContainer<PsqlContainer>("postgres:12.0")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DatabaseTest {
    val mockEnv = mockk<Environment>(relaxed = true)

    val psqlContainer =
        PsqlContainer()
            .withExposedPorts(5432)
            .withUsername("username")
            .withPassword("password")
            .withDatabaseName("databasename1")
            .withInitScript("db/testdb-init.sql")

    @BeforeAll
    fun beforeAll() {
        psqlContainer.start()
    }

    @BeforeEach
    fun beforeTest() {
        every { mockEnv.databaseUsername } returns "username"
        every { mockEnv.databasePassword } returns "password"
    }

    @Test
    internal fun `Test database should fail 4 times then connect`() {
        every { mockEnv.jdbcUrl() } returnsMany
            (0 until 4).map { "jdbc:postgresql://127.0.0.1:5433/databasename1" } andThen
            psqlContainer.jdbcUrl
        Database(mockEnv, 5, 0)
    }

    @Test
    internal fun `Test database fail after timeout exeeded`() {
        every { mockEnv.jdbcUrl() } returns "jdbc:postgresql://127.0.0.1:5433/databasename1"
        assertFailsWith<RuntimeException> { Database(mockEnv, retries = 5, sleepTime = 0) }
    }

    @Test
    internal fun `Test db queries Insert new sykmeldingStatus`() {
        every { mockEnv.jdbcUrl() } returns psqlContainer.jdbcUrl
        val database = Database(mockEnv)

        val altinnStatus = getAltinnStatus()
        database.insertAltinnStatus(altinnStatus)

        val queryById = database.getAltinnStatus(altinnStatus.id)
        queryById shouldBeEqualTo altinnStatus

        val queryBySykmeldingId = database.getAltinnStatus(altinnStatus.id)
        queryBySykmeldingId shouldBeEqualTo altinnStatus
    }

    @Test
    internal fun `Test db update status`() {
        every { mockEnv.jdbcUrl() } returns psqlContainer.jdbcUrl
        val database = Database(mockEnv)
        val altinnStatus = getAltinnStatus()
        database.insertAltinnStatus(altinnStatus)

        val updated = altinnStatus.copy(status = AltinnStatus.Status.SENDT)
        database.updateAltinnStatus(updated)

        val status = database.getAltinnStatus(altinnStatus.id)
        status!!.status shouldBeEqualTo updated.status
    }

    @Test
    internal fun `Should update status with senders reference`() {
        every { mockEnv.jdbcUrl() } returns psqlContainer.jdbcUrl
        val database = Database(mockEnv)

        val status = getAltinnStatus()
        database.insertAltinnStatus(status)

        val updataStatus = status.copy(status = AltinnStatus.Status.SENDT, sendersReference = "123")
        database.updateAltinnStatus(updataStatus)

        database.getAltinnStatus(status.id) shouldBeEqualTo updataStatus
    }

    @Test
    internal fun `erSendtSisteUke er true hvis melding er sendt for 6 dager siden`() {
        every { mockEnv.jdbcUrl() } returns psqlContainer.jdbcUrl
        val database = Database(mockEnv)

        val altinnStatus =
            getAltinnStatus()
                .copy(
                    fnr = "fnr1",
                    status = AltinnStatus.Status.SENDT,
                    timestamp = getTickMillis().minusDays(6),
                )
        database.insertAltinnStatus(altinnStatus)

        database.erSendtSisteUke(
            orgnummer = "orgnr",
            fnr = "fnr1",
            enUkeSiden = getTickMillis().minusWeeks(1),
        ) shouldBeEqualTo true
    }

    @Test
    internal fun `erSendtSisteUke er false hvis melding er sendt for 8 dager siden`() {
        every { mockEnv.jdbcUrl() } returns psqlContainer.jdbcUrl
        val database = Database(mockEnv)

        val altinnStatus =
            getAltinnStatus()
                .copy(
                    fnr = "fnr2",
                    status = AltinnStatus.Status.SENDT,
                    timestamp = getTickMillis().minusDays(8),
                )
        database.insertAltinnStatus(altinnStatus)

        database.erSendtSisteUke(
            orgnummer = "orgnr",
            fnr = "fnr2",
            enUkeSiden = getTickMillis().minusWeeks(1),
        ) shouldBeEqualTo false
    }
}

fun getTickMillis(): OffsetDateTime {
    return OffsetDateTime.now(Clock.tickMillis(ZoneOffset.UTC))
}

private fun getAltinnStatus() =
    AltinnStatus(
        id = UUID.randomUUID(),
        sykmeldingId = UUID.randomUUID(),
        orgNr = "orgnr",
        fnr = "fnr",
        timestamp = getTickMillis(),
        status = AltinnStatus.Status.NEW,
        sendersReference = null,
    )
