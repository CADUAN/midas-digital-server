package com.midasdigital.server.infrastructure.persistence

import com.midasdigital.server.domain.error.InsufficientFundsException
import com.midasdigital.server.domain.error.NotFoundException
import com.midasdigital.server.domain.model.BankAccountType
import com.midasdigital.server.domain.model.PaymentRequisitesRecord
import com.midasdigital.server.domain.model.TransferResult
import java.math.BigDecimal
import java.sql.Connection
import kotlin.math.max
import kotlin.math.min

/** Общие низкоуровневые операции БД, переиспользуемые JDBC-адаптерами. */

internal inline fun <T> Connection.runInTransaction(block: () -> T): T {
    val previousAutoCommit = autoCommit
    autoCommit = false
    return try {
        val result = block()
        commit()
        result
    } catch (ex: Exception) {
        rollback()
        throw ex
    } finally {
        autoCommit = previousAutoCommit
    }
}

internal fun transferBetweenUsers(
    connection: Connection,
    fromUserId: Long,
    recipientUserId: Long,
    amount: BigDecimal,
    note: String?,
    cardNumber: String? = null,
    contractNumber: String? = null
): TransferResult {
    if (recipientUserId == fromUserId) {
        throw NotFoundException("Нельзя перевести средства самому себе")
    }

    val firstUserId = min(fromUserId, recipientUserId)
    val secondUserId = max(fromUserId, recipientUserId)

    val balances = mutableMapOf<Long, BigDecimal>()
    connection.prepareStatement(
        """
            SELECT user_id, balance
            FROM bank_accounts
            WHERE user_id IN (?, ?)
              AND type = ?
              AND currency = ?
            ORDER BY user_id
            FOR UPDATE
        """.trimIndent()
    ).use { statement ->
        statement.setLong(1, firstUserId)
        statement.setLong(2, secondUserId)
        statement.setString(3, BankAccountType.CURRENT.name)
        statement.setString(4, "RUB")
        statement.executeQuery().use { rs ->
            while (rs.next()) {
                balances[rs.getLong("user_id")] = rs.getBigDecimal("balance")
            }
        }
    }

    val senderBalance = balances[fromUserId] ?: throw NotFoundException("Счет отправителя не найден")
    if (senderBalance < amount) {
        throw InsufficientFundsException("Недостаточно средств")
    }

    connection.prepareStatement(
        """
            UPDATE bank_accounts
            SET balance = balance - ?, updated_at = NOW()
            WHERE user_id = ? AND type = ? AND currency = ?
        """.trimIndent()
    ).use { statement ->
        statement.setBigDecimal(1, amount)
        statement.setLong(2, fromUserId)
        statement.setString(3, BankAccountType.CURRENT.name)
        statement.setString(4, "RUB")
        statement.executeUpdate()
    }

    connection.prepareStatement(
        """
            UPDATE bank_accounts
            SET balance = balance + ?, updated_at = NOW()
            WHERE user_id = ? AND type = ? AND currency = ?
        """.trimIndent()
    ).use { statement ->
        statement.setBigDecimal(1, amount)
        statement.setLong(2, recipientUserId)
        statement.setString(3, BankAccountType.CURRENT.name)
        statement.setString(4, "RUB")
        statement.executeUpdate()
    }

    val transactionId = connection.prepareStatement(
        """
            INSERT INTO transactions (sender_user_id, recipient_user_id, amount, note, recipient_card_number, contract_number)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id
        """.trimIndent()
    ).use { statement ->
        statement.setLong(1, fromUserId)
        statement.setLong(2, recipientUserId)
        statement.setBigDecimal(3, amount)
        statement.setString(4, note)
        statement.setString(5, cardNumber)
        statement.setString(6, contractNumber)
        statement.executeQuery().use { rs ->
            rs.next()
            rs.getLong("id")
        }
    }

    val balance = connection.prepareStatement(
        """
            SELECT balance
            FROM bank_accounts
            WHERE user_id = ? AND type = ? AND currency = ?
        """.trimIndent()
    ).use { statement ->
        statement.setLong(1, fromUserId)
        statement.setString(2, BankAccountType.CURRENT.name)
        statement.setString(3, "RUB")
        statement.executeQuery().use { rs ->
            rs.next()
            rs.getBigDecimal("balance")
        }
    }

    return TransferResult(transactionId = transactionId, balance = balance)
}

internal fun createBankAccounts(connection: Connection, userId: Long, initialBalance: BigDecimal) {
    val sql = """
        INSERT INTO bank_accounts (user_id, type, currency, balance)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (user_id, type, currency) DO NOTHING
    """.trimIndent()

    connection.prepareStatement(sql).use { statement ->
        statement.setLong(1, userId)
        statement.setString(2, BankAccountType.CURRENT.name)
        statement.setString(3, "RUB")
        statement.setBigDecimal(4, initialBalance)
        statement.executeUpdate()
    }

    connection.prepareStatement(sql).use { statement ->
        statement.setLong(1, userId)
        statement.setString(2, BankAccountType.SAVINGS.name)
        statement.setString(3, "RUB")
        statement.setBigDecimal(4, BigDecimal.ZERO)
        statement.executeUpdate()
    }

    connection.prepareStatement(sql).use { statement ->
        statement.setLong(1, userId)
        statement.setString(2, BankAccountType.INVESTMENT.name)
        statement.setString(3, "RUB")
        statement.setBigDecimal(4, BigDecimal.ZERO)
        statement.executeUpdate()
    }
}

internal fun createPrimaryCard(connection: Connection, userId: Long) {
    val sql = """
        INSERT INTO user_cards (user_id, card_number, is_primary)
        VALUES (?, ?, ?)
        ON CONFLICT DO NOTHING
    """.trimIndent()

    connection.prepareStatement(sql).use { statement ->
        statement.setLong(1, userId)
        statement.setString(2, generateCardNumber(connection))
        statement.setBoolean(3, true)
        statement.executeUpdate()
    }
}

private fun generateCardNumber(connection: Connection): String {
    val index = nextCardNumberIndex(connection)
    val prefix = "2200"
    val totalLength = 16
    val suffixLength = totalLength - prefix.length
    return prefix + index.toString().padStart(suffixLength, '0')
}

private fun nextCardNumberIndex(connection: Connection): Long {
    return connection.prepareStatement("SELECT nextval('card_number_seq') AS seq").use { statement ->
        statement.executeQuery().use { rs ->
            rs.next()
            rs.getLong("seq")
        }
    }
}

internal fun findPaymentRequisites(connection: Connection, userId: Long): PaymentRequisitesRecord? {
    val sql = """
        SELECT inn, kpp, bik, account, correspondent_account, contract_number
        FROM payment_requisites
        WHERE user_id = ?
    """.trimIndent()

    return connection.prepareStatement(sql).use { statement ->
        statement.setLong(1, userId)
        statement.executeQuery().use { rs ->
            if (!rs.next()) {
                return@use null
            }
            PaymentRequisitesRecord(
                inn = rs.getString("inn"),
                kpp = rs.getString("kpp"),
                bik = rs.getString("bik"),
                account = rs.getString("account"),
                correspondentAccount = rs.getString("correspondent_account"),
                contractNumber = rs.getString("contract_number")
            )
        }
    }
}

internal fun createPaymentRequisites(connection: Connection, userId: Long) {
    val requisites = generatePaymentRequisites(connection)
    val sql = """
        INSERT INTO payment_requisites (user_id, inn, kpp, bik, account, correspondent_account, contract_number)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (user_id) DO NOTHING
    """.trimIndent()

    connection.prepareStatement(sql).use { statement ->
        statement.setLong(1, userId)
        statement.setString(2, requisites.inn)
        statement.setString(3, requisites.kpp)
        statement.setString(4, requisites.bik)
        statement.setString(5, requisites.account)
        statement.setString(6, requisites.correspondentAccount)
        statement.setString(7, requisites.contractNumber)
        statement.executeUpdate()
    }
}

private fun generatePaymentRequisites(connection: Connection): PaymentRequisitesRecord {
    val index = nextPaymentRequisitesIndex(connection)
    return PaymentRequisitesRecord(
        inn = buildRequisitesValue(prefix = "770", totalLength = 10, index = index),
        kpp = buildRequisitesValue(prefix = "7701", totalLength = 9, index = index),
        bik = buildRequisitesValue(prefix = "044", totalLength = 9, index = index),
        account = buildRequisitesValue(prefix = "40702810", totalLength = 20, index = index),
        correspondentAccount = buildRequisitesValue(prefix = "30101810", totalLength = 20, index = index),
        contractNumber = buildRequisitesValue(prefix = "57267", totalLength = 11, index = index)
    )
}

private fun nextPaymentRequisitesIndex(connection: Connection): Long {
    return connection.prepareStatement("SELECT nextval('payment_requisites_seq') AS seq").use { statement ->
        statement.executeQuery().use { rs ->
            rs.next()
            rs.getLong("seq")
        }
    }
}

private fun buildRequisitesValue(prefix: String, totalLength: Int, index: Long): String {
    val suffixLength = totalLength - prefix.length
    val rawSuffix = index.toString()
    val suffix = if (suffixLength > 0) rawSuffix.padStart(suffixLength, '0') else rawSuffix
    return prefix + suffix
}
