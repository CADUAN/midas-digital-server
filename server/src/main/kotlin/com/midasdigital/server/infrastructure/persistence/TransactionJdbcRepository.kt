package com.midasdigital.server.infrastructure.persistence

import com.midasdigital.server.domain.error.NotFoundException
import com.midasdigital.server.domain.model.Transaction
import com.midasdigital.server.domain.model.TransferResult
import com.midasdigital.server.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.ZoneOffset

class TransactionJdbcRepository : TransactionRepository {

    override fun transfer(
        fromUserId: Long,
        recipientPhone: String,
        amount: BigDecimal,
        note: String?
    ): TransferResult {
        return DatabaseFactory.withConnection { connection ->
            connection.runInTransaction {
                val recipient = connection.prepareStatement(
                    """
                        SELECT id
                        FROM users
                        WHERE phone = ?
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, recipientPhone)
                    statement.executeQuery().use { rs ->
                        if (!rs.next()) {
                            throw NotFoundException("Получатель не найден")
                        }
                        rs.getLong("id")
                    }
                }
                transferBetweenUsers(connection, fromUserId, recipient, amount, note)
            }
        }
    }

    override fun transferByUserId(
        fromUserId: Long,
        recipientUserId: Long,
        amount: BigDecimal,
        note: String?,
        cardNumber: String?,
        contractNumber: String?
    ): TransferResult {
        return DatabaseFactory.withConnection { connection ->
            connection.runInTransaction {
                transferBetweenUsers(connection, fromUserId, recipientUserId, amount, note, cardNumber, contractNumber)
            }
        }
    }

    override fun listTransactions(userId: Long): List<Transaction> {
        val sql = """
            SELECT
                t.id,
                t.sender_user_id,
                su.phone AS sender_phone,
                t.recipient_user_id,
                ru.phone AS recipient_phone,
                t.amount,
                t.note,
                t.recipient_card_number,
                t.contract_number,
                t.created_at
            FROM transactions t
            JOIN users su ON su.id = t.sender_user_id
            JOIN users ru ON ru.id = t.recipient_user_id
            WHERE t.sender_user_id = ? OR t.recipient_user_id = ?
            ORDER BY t.created_at DESC
            LIMIT 100
        """.trimIndent()

        return DatabaseFactory.withConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, userId)
                statement.setLong(2, userId)
                statement.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            val incoming = rs.getLong("recipient_user_id") == userId
                            add(
                                Transaction(
                                    id = rs.getLong("id"),
                                    isIncoming = incoming,
                                    counterpartyPhone = if (incoming) {
                                        rs.getString("sender_phone")
                                    } else {
                                        rs.getString("recipient_phone")
                                    },
                                    amount = rs.getBigDecimal("amount"),
                                    note = rs.getString("note"),
                                    cardNumber = rs.getString("recipient_card_number"),
                                    contractNumber = rs.getString("contract_number"),
                                    createdAtIso = rs.getTimestamp("created_at")
                                        ?.toInstant()
                                        ?.toString()
                                        ?: Timestamp.from(OffsetDateTime.now(ZoneOffset.UTC).toInstant())
                                            .toInstant()
                                            .toString()
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
