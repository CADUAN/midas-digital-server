package com.midasdigital.server.infrastructure.persistence

import com.midasdigital.server.domain.model.SessionUser
import com.midasdigital.server.domain.repository.SessionRepository
import com.midasdigital.server.infrastructure.config.AppConfig
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class SessionJdbcRepository(private val config: AppConfig) : SessionRepository {

    override fun createSession(userId: Long): String {
        val token = UUID.randomUUID()
        val expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(config.sessionTtlDays)
        val sql = """
            INSERT INTO sessions (token, user_id, expires_at)
            VALUES (?, ?, ?)
        """.trimIndent()

        DatabaseFactory.withConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, token)
                statement.setLong(2, userId)
                statement.setObject(3, expiresAt)
                statement.executeUpdate()
            }
        }

        return token.toString()
    }

    override fun findUserByToken(token: String): SessionUser? {
        val tokenUuid = runCatching { UUID.fromString(token) }.getOrNull() ?: return null
        val sql = """
            SELECT u.id, u.full_name, u.phone
            FROM sessions s
            JOIN users u ON u.id = s.user_id
            WHERE s.token = ?
              AND s.expires_at > NOW()
        """.trimIndent()

        return DatabaseFactory.withConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, tokenUuid)
                statement.executeQuery().use { rs ->
                    if (!rs.next()) {
                        return@withConnection null
                    }
                    SessionUser(
                        id = rs.getLong("id"),
                        fullName = rs.getString("full_name"),
                        phone = rs.getString("phone")
                    )
                }
            }
        }
    }
}
