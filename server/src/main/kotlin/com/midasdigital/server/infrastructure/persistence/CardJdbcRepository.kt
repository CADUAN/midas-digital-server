package com.midasdigital.server.infrastructure.persistence

import com.midasdigital.server.domain.model.Card
import com.midasdigital.server.domain.repository.CardRepository
import com.midasdigital.server.domain.service.CardCredentials
import java.time.OffsetDateTime
import java.time.ZoneOffset

class CardJdbcRepository : CardRepository {

    override fun listCards(userId: Long): List<Card> {
        val sql = """
            SELECT id, card_number, is_primary, created_at
            FROM user_cards
            WHERE user_id = ?
            ORDER BY id
        """.trimIndent()

        return DatabaseFactory.withConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, userId)
                statement.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            val cardNumber = rs.getString("card_number")
                            val issuedAt = rs.getTimestamp("created_at")
                                ?.toInstant()
                                ?.atOffset(ZoneOffset.UTC)
                                ?: OffsetDateTime.now(ZoneOffset.UTC)
                            add(
                                Card(
                                    id = rs.getLong("id"),
                                    cardNumber = cardNumber,
                                    isPrimary = rs.getBoolean("is_primary"),
                                    expiry = CardCredentials.expiry(issuedAt),
                                    cvv = CardCredentials.cvv(cardNumber)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
