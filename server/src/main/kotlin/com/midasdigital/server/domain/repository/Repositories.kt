package com.midasdigital.server.domain.repository

import com.midasdigital.server.domain.model.AccountTransferResult
import com.midasdigital.server.domain.model.BankAccount
import com.midasdigital.server.domain.model.BankAccountType
import com.midasdigital.server.domain.model.Card
import com.midasdigital.server.domain.model.CurrencyConvertResult
import com.midasdigital.server.domain.model.CurrencyRate
import com.midasdigital.server.domain.model.PaymentRequisitesRecord
import com.midasdigital.server.domain.model.SessionUser
import com.midasdigital.server.domain.model.Transaction
import com.midasdigital.server.domain.model.TransferResult
import com.midasdigital.server.domain.model.User
import java.math.BigDecimal

interface UserRepository {
    fun findUserByPhone(phone: String): User?
    fun createUser(fullName: String, phone: String, pinHash: String): User
    fun findUserIdByCardNumber(cardNumber: String): Long?
    fun findUserIdByRequisites(requisites: PaymentRequisitesRecord): Long?
}

interface SessionRepository {
    fun createSession(userId: Long): String
    fun findUserByToken(token: String): SessionUser?
}

interface AccountRepository {
    fun getBalance(userId: Long): BigDecimal
    fun listAccounts(userId: Long): List<BankAccount>
    fun transferBetweenAccounts(
        userId: Long,
        fromType: BankAccountType,
        toType: BankAccountType,
        currency: String,
        amount: BigDecimal
    ): AccountTransferResult
    fun convertCurrency(
        userId: Long,
        fromCurrency: String,
        toCurrency: String,
        amount: BigDecimal
    ): CurrencyConvertResult
}

interface CardRepository {
    fun listCards(userId: Long): List<Card>
}

interface RequisitesRepository {
    fun getPaymentRequisites(userId: Long): PaymentRequisitesRecord
}

interface CurrencyRepository {
    fun listCurrencyRates(): List<CurrencyRate>
    fun upsertRate(currency: String, rateToRub: BigDecimal)
}

/** Внешний источник котировок (Yahoo Finance). Отдаёт курс валюты к рублю. */
interface ExchangeRateSource {
    fun fetchRatesToRub(currencies: List<String>): Map<String, BigDecimal>
}

interface TransactionRepository {
    fun transfer(fromUserId: Long, recipientPhone: String, amount: BigDecimal, note: String?): TransferResult
    fun transferByUserId(
        fromUserId: Long,
        recipientUserId: Long,
        amount: BigDecimal,
        note: String?,
        cardNumber: String? = null,
        contractNumber: String? = null
    ): TransferResult
    fun listTransactions(userId: Long): List<Transaction>
}
