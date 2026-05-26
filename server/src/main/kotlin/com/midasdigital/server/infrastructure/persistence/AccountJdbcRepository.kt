package com.midasdigital.server.infrastructure.persistence

import com.midasdigital.server.domain.error.InsufficientFundsException
import com.midasdigital.server.domain.error.NotFoundException
import com.midasdigital.server.domain.model.AccountTransferResult
import com.midasdigital.server.domain.model.BankAccount
import com.midasdigital.server.domain.model.BankAccountType
import com.midasdigital.server.domain.model.CurrencyConvertResult
import com.midasdigital.server.domain.repository.AccountRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.sql.Connection

class AccountJdbcRepository : AccountRepository {

    override fun getBalance(userId: Long): BigDecimal {
        val sql = """
            SELECT balance
            FROM bank_accounts
            WHERE user_id = ?
              AND type = ?
              AND currency = ?
        """.trimIndent()

        return DatabaseFactory.withConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, userId)
                statement.setString(2, BankAccountType.CURRENT.name)
                statement.setString(3, "RUB")
                statement.executeQuery().use { rs ->
                    if (!rs.next()) {
                        throw NotFoundException("Кошелёк не найден")
                    }
                    rs.getBigDecimal("balance")
                }
            }
        }
    }

    override fun listAccounts(userId: Long): List<BankAccount> {
        val sql = """
            SELECT id, type, currency, balance
            FROM bank_accounts
            WHERE user_id = ?
            ORDER BY type, currency
        """.trimIndent()

        return DatabaseFactory.withConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, userId)
                statement.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                BankAccount(
                                    id = rs.getLong("id"),
                                    type = BankAccountType.valueOf(rs.getString("type")),
                                    currency = rs.getString("currency"),
                                    balance = rs.getBigDecimal("balance")
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override fun transferBetweenAccounts(
        userId: Long,
        fromType: BankAccountType,
        toType: BankAccountType,
        currency: String,
        amount: BigDecimal
    ): AccountTransferResult {
        return DatabaseFactory.withConnection { connection ->
            connection.runInTransaction {
                val normalizedCurrency = currency.uppercase()
                val accounts = findAccountsForUpdate(connection, userId, fromType, toType, normalizedCurrency)
                val fromAccount = accounts[fromType]
                    ?: throw NotFoundException("Счет списания не найден")
                val toAccount = accounts[toType]
                    ?: throw NotFoundException("Счет зачисления не найден")

                if (fromAccount.balance < amount) {
                    throw InsufficientFundsException("Недостаточно средств")
                }

                updateAccountBalance(connection, fromAccount.id, fromAccount.balance - amount)
                updateAccountBalance(connection, toAccount.id, toAccount.balance + amount)

                AccountTransferResult(
                    fromBalance = fromAccount.balance - amount,
                    toBalance = toAccount.balance + amount
                )
            }
        }
    }

    override fun convertCurrency(
        userId: Long,
        fromCurrency: String,
        toCurrency: String,
        amount: BigDecimal
    ): CurrencyConvertResult {
        return DatabaseFactory.withConnection { connection ->
            connection.runInTransaction {
                val normalizedFrom = fromCurrency.uppercase()
                val normalizedTo = toCurrency.uppercase()
                if (normalizedFrom == normalizedTo) {
                    throw NotFoundException("Валюты должны отличаться")
                }

                val fromAccount = findAccountForUpdate(connection, userId, BankAccountType.CURRENT, normalizedFrom)
                    ?: throw NotFoundException("Счет списания не найден")

                val toAccount = findAccountForUpdate(connection, userId, BankAccountType.CURRENT, normalizedTo)
                    ?: createAccount(connection, userId, BankAccountType.CURRENT, normalizedTo, BigDecimal.ZERO)

                val fromRate = findCurrencyRate(connection, normalizedFrom)
                    ?: throw NotFoundException("Курс валюты не найден")
                val toRate = findCurrencyRate(connection, normalizedTo)
                    ?: throw NotFoundException("Курс валюты не найден")

                if (fromAccount.balance < amount) {
                    throw InsufficientFundsException("Недостаточно средств")
                }

                val amountRub = amount.multiply(fromRate)
                val convertedAmount = amountRub.divide(toRate, 2, RoundingMode.HALF_EVEN)

                updateAccountBalance(connection, fromAccount.id, fromAccount.balance - amount)
                updateAccountBalance(connection, toAccount.id, toAccount.balance + convertedAmount)

                val rate = convertedAmount.divide(amount, 6, RoundingMode.HALF_EVEN)

                CurrencyConvertResult(
                    convertedAmount = convertedAmount,
                    rate = rate,
                    fromBalance = fromAccount.balance - amount,
                    toBalance = toAccount.balance + convertedAmount
                )
            }
        }
    }
}

private fun createAccount(
    connection: Connection,
    userId: Long,
    type: BankAccountType,
    currency: String,
    balance: BigDecimal
): BankAccount {
    val sql = """
        INSERT INTO bank_accounts (user_id, type, currency, balance)
        VALUES (?, ?, ?, ?)
        ON CONFLICT (user_id, type, currency) DO NOTHING
    """.trimIndent()

    connection.prepareStatement(sql).use { statement ->
        statement.setLong(1, userId)
        statement.setString(2, type.name)
        statement.setString(3, currency)
        statement.setBigDecimal(4, balance)
        statement.executeUpdate()
    }

    return findAccountForUpdate(connection, userId, type, currency)
        ?: throw NotFoundException("Счет не найден")
}

private fun findAccountForUpdate(
    connection: Connection,
    userId: Long,
    type: BankAccountType,
    currency: String
): BankAccount? {
    val sql = """
        SELECT id, type, currency, balance
        FROM bank_accounts
        WHERE user_id = ?
          AND type = ?
          AND currency = ?
        FOR UPDATE
    """.trimIndent()

    return connection.prepareStatement(sql).use { statement ->
        statement.setLong(1, userId)
        statement.setString(2, type.name)
        statement.setString(3, currency)
        statement.executeQuery().use { rs ->
            if (!rs.next()) {
                return@use null
            }
            BankAccount(
                id = rs.getLong("id"),
                type = BankAccountType.valueOf(rs.getString("type")),
                currency = rs.getString("currency"),
                balance = rs.getBigDecimal("balance")
            )
        }
    }
}

private fun findAccountsForUpdate(
    connection: Connection,
    userId: Long,
    fromType: BankAccountType,
    toType: BankAccountType,
    currency: String
): Map<BankAccountType, BankAccount> {
    val sql = """
        SELECT id, type, currency, balance
        FROM bank_accounts
        WHERE user_id = ?
          AND type IN (?, ?)
          AND currency = ?
        FOR UPDATE
    """.trimIndent()

    return connection.prepareStatement(sql).use { statement ->
        statement.setLong(1, userId)
        statement.setString(2, fromType.name)
        statement.setString(3, toType.name)
        statement.setString(4, currency)
        statement.executeQuery().use { rs ->
            buildMap {
                while (rs.next()) {
                    val type = BankAccountType.valueOf(rs.getString("type"))
                    put(
                        type,
                        BankAccount(
                            id = rs.getLong("id"),
                            type = type,
                            currency = rs.getString("currency"),
                            balance = rs.getBigDecimal("balance")
                        )
                    )
                }
            }
        }
    }
}

private fun updateAccountBalance(connection: Connection, accountId: Long, newBalance: BigDecimal) {
    val sql = """
        UPDATE bank_accounts
        SET balance = ?, updated_at = NOW()
        WHERE id = ?
    """.trimIndent()

    connection.prepareStatement(sql).use { statement ->
        statement.setBigDecimal(1, newBalance)
        statement.setLong(2, accountId)
        statement.executeUpdate()
    }
}

private fun findCurrencyRate(connection: Connection, currency: String): BigDecimal? {
    val sql = """
        SELECT rate_to_rub
        FROM currency_rates
        WHERE currency = ?
    """.trimIndent()

    return connection.prepareStatement(sql).use { statement ->
        statement.setString(1, currency)
        statement.executeQuery().use { rs ->
            if (!rs.next()) {
                return@use null
            }
            rs.getBigDecimal("rate_to_rub")
        }
    }
}
