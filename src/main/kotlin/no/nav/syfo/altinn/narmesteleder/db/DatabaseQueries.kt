package no.nav.syfo.altinn.narmesteleder.db

import no.nav.syfo.altinn.narmesteleder.model.AltinnStatus
import no.nav.syfo.db.DatabaseInterface
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.ZoneOffset
import java.util.UUID

fun DatabaseInterface.getAltinnStatus(id: UUID): AltinnStatus? {
    return connection.use { connection ->
        connection.prepareStatement(
            """
            SELECT * FROM status WHERE id = ?
            """.trimIndent()
        ).use {
            it.setObject(1, id)
            it.executeQuery().toAltinnStatus()
        }
    }
}

fun DatabaseInterface.getAltinnStatusBySykmeldingId(sykmeldingId: UUID): AltinnStatus? {
    return connection.use { connection ->
        connection.prepareStatement(
            """
            SELECT * FROM status WHERE sykmelding_id = ?
            """.trimIndent()
        ).use {
            it.setObject(1, sykmeldingId)
            it.executeQuery().toAltinnStatus()
        }
    }
}

fun DatabaseInterface.insertAltinnStatus(altinnStatus: AltinnStatus) {
    connection.use { connection ->
        connection.prepareStatement(
            """
            INSERT INTO status(id, sykmelding_id, org_nr, fnr, timestamp, status)
             VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { ps ->
            var i = 1
            ps.setObject(i++, altinnStatus.id)
            ps.setObject(i++, altinnStatus.sykmeldingId)
            ps.setString(i++, altinnStatus.orgNr)
            ps.setString(i++, altinnStatus.fnr)
            ps.setTimestamp(i++, Timestamp.from(altinnStatus.timestamp.toInstant()))
            ps.setString(i, altinnStatus.status.toString())
            ps.executeUpdate()
        }
        connection.commit()
    }
}

fun DatabaseInterface.updateAltinnStatus(altinnStatus: AltinnStatus) {
    connection.use { connection ->
        connection.prepareStatement(
            """
           UPDATE status 
           SET status = ? 
           WHERE id = ?
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, altinnStatus.status.name)
            ps.setObject(2, altinnStatus.id)
            ps.executeUpdate()
        }
        connection.commit()
    }
}

private fun ResultSet.toAltinnStatus(): AltinnStatus? {
    return when (next()) {
        true -> AltinnStatus(
            id = UUID.fromString(getString("id")),
            sykmeldingId = UUID.fromString(getString("sykmelding_id")),
            orgNr = getString("org_nr"),
            fnr = getString("fnr"),
            timestamp = getTimestamp("timestamp").toInstant().atOffset(ZoneOffset.UTC),
            status = AltinnStatus.Status.valueOf(getString("status"))

        )
        false -> null
    }
}
