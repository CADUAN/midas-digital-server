package com.midasdigital.server.infrastructure.persistence

import com.midasdigital.server.domain.model.PaymentRequisitesRecord
import com.midasdigital.server.domain.model.User
import com.midasdigital.server.domain.repository.UserRepository
import com.midasdigital.server.infrastructure.config.AppConfig

class UserJdbcRepository(private val config: AppConfig) : UserRepository {

    override fun findUserByPhone(phone: String): User? {
        val sql = """
            SELECT id, full_name, phone, pin_hash
            FROM users
            WHERE phone = ?
        """.trimIndent()

        return DatabaseFactory.withConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, phone)
                statement.executeQuery().use { rs ->
                    if (!rs.next()) {
                        return@withConnection null
                    }
                    User(
                        id = rs.getLong("id"),
                        fullName = rs.getString("full_name"),
                        phone = rs.getString("phone"),
                        pinHash = rs.getString("pin_hash")
                    )
                }
            }
        }
    }

    override fun createUser(fullName: String, phone: String, pinHash: String): User {
        return DatabaseFactory.withConnection { connection ->
            connection.runInTransaction {
                val userId = connection.prepareStatement(
                    """
                        INSERT INTO users (full_name, phone, pin_hash)
                        VALUES (?, ?, ?)
                        RETURNING id
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, fullName)
                    statement.setString(2, phone)
                    statement.setString(3, pinHash)
                    statement.executeQuery().use { rs ->
                        rs.next()
                        rs.getLong("id")
                    }
                }

                createBankAccounts(connection, userId, config.demoInitialBalance)
                createPrimaryCard(connection, userId)
                createPaymentRequisites(connection, userId)

                User(id = userId, fullName = fullName, phone = phone, pinHash = pinHash)
            }
        }
    }

    override fun findUserIdByCardNumber(cardNumber: String): Long? {
        val sql = """
            SELECT user_id
            FROM user_cards
            WHERE card_number = ?
        """.trimIndent()

        return DatabaseFactory.withConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, cardNumber)
                statement.executeQuery().use { rs ->
                    if (!rs.next()) {
                        return@withConnection null
                    }
                    rs.getLong("user_id")
                }
            }
        }
    }

    override fun findUserIdByRequisites(requisites: PaymentRequisitesRecord): Long? {
        val sql = """
            SELECT user_id
            FROM payment_requisites
            WHERE inn = ?
              AND kpp = ?
              AND bik = ?
              AND account = ?
              AND correspondent_account = ?
              AND contract_number = ?
        """.trimIndent()

        return DatabaseFactory.withConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, requisites.inn)
                statement.setString(2, requisites.kpp)
                statement.setString(3, requisites.bik)
                statement.setString(4, requisites.account)
                statement.setString(5, requisites.correspondentAccount)
                statement.setString(6, requisites.contractNumber)
                statement.executeQuery().use { rs ->
                    if (!rs.next()) {
                        return@withConnection null
                    }
                    rs.getLong("user_id")
                }
            }
        }
    }
}
