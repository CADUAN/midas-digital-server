package com.midasdigital.server.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class RegisterRequest(
    val fullName: String,
    val phone: String,
    val pin: String
)

@Serializable
data class LoginRequest(
    val phone: String,
    val pin: String
)

@Serializable
data class UserResponse(
    val id: Long,
    val fullName: String,
    val phone: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResponse
)

@Serializable
data class BalanceResponse(
    val balance: String,
    val currency: String = "RUB"
)

@Serializable
data class BankAccountResponse(
    val id: Long,
    val type: String,
    val currency: String,
    val balance: String
)

@Serializable
data class BankAccountsResponse(
    val accounts: List<BankAccountResponse>
)

@Serializable
data class AccountTransferRequest(
    val fromType: String,
    val toType: String,
    val amount: String,
    val currency: String = "RUB"
)

@Serializable
data class AccountTransferResponse(
    val fromBalance: String,
    val toBalance: String,
    val currency: String
)

@Serializable
data class CardResponse(
    val id: Long,
    val cardNumber: String,
    val isPrimary: Boolean,
    val expiry: String,
    val cvv: String
)

@Serializable
data class CardsResponse(
    val cards: List<CardResponse>
)

@Serializable
data class PaymentRequisites(
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

@Serializable
data class PaymentRequisitesResponse(
    val requisites: PaymentRequisites,
    val qrPayload: String
)

@Serializable
data class TransferRequest(
    val recipientPhone: String,
    val amount: String,
    val note: String? = null
)

@Serializable
data class TransferByCardRequest(
    val cardNumber: String,
    val amount: String,
    val note: String? = null
)

@Serializable
data class TransferByRequisitesRequest(
    val inn: String,
    val kpp: String,
    val bik: String,
    val account: String,
    val correspondentAccount: String,
    val contractNumber: String,
    val amount: String,
    val note: String? = null
)

@Serializable
data class TransferByQrRequest(
    val qrPayload: String,
    val amount: String,
    val note: String? = null
)

@Serializable
data class TransferResponse(
    val transactionId: Long,
    val balance: String
)

@Serializable
data class TransactionItem(
    val id: Long,
    val direction: String,
    val counterpartyPhone: String,
    val amount: String,
    val note: String? = null,
    val createdAt: String,
    val cardNumber: String? = null,
    val contractNumber: String? = null
)

@Serializable
data class TransactionsResponse(
    val items: List<TransactionItem>
)

@Serializable
data class CurrencyRateResponse(
    val currency: String,
    val rateToRub: String,
    val updatedAt: String
)

@Serializable
data class CurrencyRatesResponse(
    val base: String = "RUB",
    val rates: List<CurrencyRateResponse>
)

@Serializable
data class CurrencyConvertRequest(
    val fromCurrency: String,
    val toCurrency: String,
    val amount: String
)

@Serializable
data class CurrencyConvertResponse(
    val fromCurrency: String,
    val toCurrency: String,
    val amount: String,
    val convertedAmount: String,
    val rate: String,
    val fromBalance: String,
    val toBalance: String
)
