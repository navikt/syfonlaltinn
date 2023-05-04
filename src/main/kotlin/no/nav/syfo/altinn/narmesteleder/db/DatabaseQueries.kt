package no.nav.syfo.altinn.narmesteleder.db

import no.nav.syfo.altinn.narmesteleder.model.AltinnStatus
import no.nav.syfo.db.DatabaseInterface
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

fun DatabaseInterface.getAltinnStatus(id: UUID): AltinnStatus? {
    return connection.use { connection ->
        connection.prepareStatement(
            """
            SELECT * FROM status WHERE id = ?
            """,
        ).use {
            it.setObject(1, id)
            it.executeQuery().toAltinnStatus()
        }
    }
}

fun DatabaseInterface.insertAltinnStatus(altinnStatus: AltinnStatus) {
    connection.use { connection ->
        connection.prepareStatement(
            """
            INSERT INTO status(id, sykmelding_id, org_nr, fnr, timestamp, status, senders_reference)
             VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
        ).use { ps ->
            var i = 1
            ps.setObject(i++, altinnStatus.id)
            ps.setObject(i++, altinnStatus.sykmeldingId)
            ps.setString(i++, altinnStatus.orgNr)
            ps.setString(i++, altinnStatus.fnr)
            ps.setTimestamp(i++, Timestamp.from(altinnStatus.timestamp.toInstant()))
            ps.setString(i++, altinnStatus.status.toString())
            ps.setString(i, altinnStatus.sendersReference)
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
           SET status = ?,
           senders_reference = ?
           WHERE id = ?
            """,
        ).use { ps ->
            ps.setString(1, altinnStatus.status.name)
            ps.setString(2, altinnStatus.sendersReference)
            ps.setObject(3, altinnStatus.id)
            ps.executeUpdate()
        }
        connection.commit()
    }
}

fun DatabaseInterface.erSendtSisteUke(orgnummer: String, fnr: String, enUkeSiden: OffsetDateTime): Boolean =
    connection.use { connection ->
        connection.prepareStatement(
            """
            SELECT * FROM status WHERE org_nr = ? AND fnr = ? AND status = 'SENDT' AND timestamp > ?
            """,
        ).use { ps ->
            ps.setString(1, orgnummer)
            ps.setString(2, fnr)
            ps.setTimestamp(3, Timestamp.from(enUkeSiden.toInstant()))
            ps.executeQuery().next()
        }
    }

private fun ResultSet.toAltinnStatus(): AltinnStatus? {
    return when (next()) {
        true -> AltinnStatus(
            id = UUID.fromString(getString("id")),
            sykmeldingId = getString("sykmelding_id")?.let { UUID.fromString(it) },
            orgNr = getString("org_nr"),
            fnr = getString("fnr"),
            timestamp = getTimestamp("timestamp").toInstant().atOffset(ZoneOffset.UTC),
            status = AltinnStatus.Status.valueOf(getString("status")),
            sendersReference = getString("senders_reference"),
        )
        false -> null
    }
}
