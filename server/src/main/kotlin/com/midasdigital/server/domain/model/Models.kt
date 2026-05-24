package com.midasdigital.server.domain.model

import java.math.BigDecimal

/** Тип банковского счёта. Чистая доменная сущность без привязки к сериализации. */
enum class BankAccountType {
    CURRENT,
    SAVINGS,
    INVESTMENT
}

data class User(
    val id: Long,
    val fullName: String,
    val phone: String,
    val pinHash: String
)

/** Пользователь сессии без чувствительных данных. */
data class SessionUser(
    val id: Long,
    val fullName: String,
    val phone: String
)

data class BankAccount(
    val id: Long,
    val type: BankAccountType,
    val currency: String,
    val balance: BigDecimal
)

data class Card(
    val id: Long,
    val cardNumber: String,
    val isPrimary: Boolean,
    val expiry: String,
    val cvv: String
)

/** Реквизиты в том виде, в котором они хранятся. */
data class PaymentRequisitesRecord(
    val inn: String,
    val kpp: String,
    val bik: String,
    val account: String,
    val correspondentAccount: String,
    val contractNumber: String
)

/** Обогащённые реквизиты для отображения (с получателем, банком, назначением). */
data class RequisitesDetails(
    val recipient: String,
    val inn: String,
    val kpp: String,
    val bik: String,
    val account: String,
    val correspondentAccount: String,
    val bank: String,
    val paymentPurpose: String,
    val contractNumber: String
)

data class CurrencyRate(
    val currency: String,
    val rateToRub: BigDecimal,
    val updatedAtIso: String
)

data class Transaction(
    val id: Long,
    val isIncoming: Boolean,
    val counterpartyPhone: String,
    val amount: BigDecimal,
    val note: String?,
    val createdAtIso: String,
    val cardNumber: String? = null,
    val contractNumber: String? = null
)

data class TransferResult(
    val transactionId: Long,
    val balance: BigDecimal
)

data class AccountTransferResult(
    val fromBalance: BigDecimal,
    val toBalance: BigDecimal
)

data class CurrencyConvertResult(
    val convertedAmount: BigDecimal,
    val rate: BigDecimal,
    val fromBalance: BigDecimal,
    val toBalance: BigDecimal
)
