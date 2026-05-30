package com.midasdigital.server.presentation.mapper

import com.midasdigital.server.application.usecase.AccountTransferOutcome
import com.midasdigital.server.application.usecase.AuthResult
import com.midasdigital.server.application.usecase.CurrencyConversion
import com.midasdigital.server.application.usecase.RequisitesWithQr
import com.midasdigital.server.domain.error.ValidationException
import com.midasdigital.server.domain.model.BankAccount
import com.midasdigital.server.domain.model.BankAccountType
import com.midasdigital.server.domain.model.Card
import com.midasdigital.server.domain.model.CurrencyRate
import com.midasdigital.server.domain.model.Transaction
import com.midasdigital.server.domain.model.TransferResult
import com.midasdigital.server.domain.service.Money
import com.midasdigital.server.presentation.dto.AccountTransferResponse
import com.midasdigital.server.presentation.dto.AuthResponse
import com.midasdigital.server.presentation.dto.BalanceResponse
import com.midasdigital.server.presentation.dto.BankAccountResponse
import com.midasdigital.server.presentation.dto.BankAccountsResponse
import com.midasdigital.server.presentation.dto.CardResponse
import com.midasdigital.server.presentation.dto.CardsResponse
import com.midasdigital.server.presentation.dto.CurrencyConvertResponse
import com.midasdigital.server.presentation.dto.CurrencyRateResponse
import com.midasdigital.server.presentation.dto.CurrencyRatesResponse
import com.midasdigital.server.presentation.dto.PaymentRequisites
import com.midasdigital.server.presentation.dto.PaymentRequisitesResponse
import com.midasdigital.server.presentation.dto.TransactionItem
import com.midasdigital.server.presentation.dto.TransactionsResponse
import com.midasdigital.server.presentation.dto.TransferResponse
import com.midasdigital.server.presentation.dto.UserResponse
import java.math.BigDecimal

fun String.toBankAccountType(): BankAccountType =
    runCatching { BankAccountType.valueOf(trim().uppercase()) }
        .getOrElse { throw ValidationException("Некорректный тип счёта") }

fun AuthResult.toResponse(): AuthResponse = AuthResponse(
    token = token,
    user = UserResponse(id = user.id, fullName = user.fullName, phone = user.phone)
)

fun BigDecimal.toBalanceResponse(): BalanceResponse = BalanceResponse(balance = Money.format(this))

fun List<BankAccount>.toAccountsResponse(): BankAccountsResponse = BankAccountsResponse(
    accounts = map { account ->
        BankAccountResponse(
            id = account.id,
            type = account.type.name,
            currency = account.currency,
            balance = Money.format(account.balance)
        )
    }
)

fun AccountTransferOutcome.toResponse(): AccountTransferResponse = AccountTransferResponse(
    fromBalance = Money.format(fromBalance),
    toBalance = Money.format(toBalance),
    currency = currency
)

fun List<Card>.toCardsResponse(): CardsResponse = CardsResponse(
    cards = map {
        CardResponse(
            id = it.id,
            cardNumber = it.cardNumber,
            isPrimary = it.isPrimary,
            expiry = it.expiry,
            cvv = it.cvv
        )
    }
)

fun RequisitesWithQr.toResponse(): PaymentRequisitesResponse = PaymentRequisitesResponse(
    requisites = PaymentRequisites(
        recipient = details.recipient,
        inn = details.inn,
        kpp = details.kpp,
        bik = details.bik,
        account = details.account,
        correspondentAccount = details.correspondentAccount,
        bank = details.bank,
        paymentPurpose = details.paymentPurpose,
        contractNumber = details.contractNumber
    ),
    qrPayload = qrPayload
)

fun TransferResult.toResponse(): TransferResponse = TransferResponse(
    transactionId = transactionId,
    balance = Money.format(balance)
)

fun List<Transaction>.toTransactionsResponse(): TransactionsResponse = TransactionsResponse(
    items = map { transaction ->
        TransactionItem(
            id = transaction.id,
            direction = if (transaction.isIncoming) "INCOMING" else "OUTGOING",
            counterpartyPhone = transaction.counterpartyPhone,
            amount = Money.format(transaction.amount),
            note = transaction.note,
            createdAt = transaction.createdAtIso,
            cardNumber = transaction.cardNumber,
            contractNumber = transaction.contractNumber
        )
    }
)

fun List<CurrencyRate>.toRatesResponse(): CurrencyRatesResponse = CurrencyRatesResponse(
    rates = map { rate ->
        CurrencyRateResponse(
            currency = rate.currency,
            rateToRub = Money.formatRate(rate.rateToRub),
            updatedAt = rate.updatedAtIso
        )
    }
)

fun CurrencyConversion.toResponse(): CurrencyConvertResponse = CurrencyConvertResponse(
    fromCurrency = fromCurrency,
    toCurrency = toCurrency,
    amount = Money.format(amount),
    convertedAmount = Money.format(convertedAmount),
    rate = Money.formatRate(rate),
    fromBalance = Money.format(fromBalance),
    toBalance = Money.format(toBalance)
)
