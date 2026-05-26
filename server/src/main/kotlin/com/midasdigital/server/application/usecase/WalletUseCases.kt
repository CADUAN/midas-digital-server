package com.midasdigital.server.application.usecase

import com.midasdigital.server.domain.error.ValidationException
import com.midasdigital.server.domain.model.BankAccount
import com.midasdigital.server.domain.model.BankAccountType
import com.midasdigital.server.domain.model.CurrencyRate
import com.midasdigital.server.domain.model.RequisitesDetails
import com.midasdigital.server.domain.repository.AccountRepository
import com.midasdigital.server.domain.repository.CardRepository
import com.midasdigital.server.domain.repository.CurrencyRepository
import com.midasdigital.server.domain.repository.RequisitesRepository
import com.midasdigital.server.domain.repository.ExchangeRateSource
import com.midasdigital.server.domain.service.InputValidators
import com.midasdigital.server.domain.service.QrPayloadCodec
import com.midasdigital.server.domain.service.RateFluctuation
import com.midasdigital.server.domain.model.Card
import java.math.BigDecimal
import java.time.Instant

/** Итог перевода между своими счетами (с указанием валюты операции). */
data class AccountTransferOutcome(
    val fromBalance: BigDecimal,
    val toBalance: BigDecimal,
    val currency: String
)

/** Итог конвертации валют со всеми деталями для ответа. */
data class CurrencyConversion(
    val fromCurrency: String,
    val toCurrency: String,
    val amount: BigDecimal,
    val convertedAmount: BigDecimal,
    val rate: BigDecimal,
    val fromBalance: BigDecimal,
    val toBalance: BigDecimal
)

/** Реквизиты вместе со сформированной QR-нагрузкой. */
data class RequisitesWithQr(
    val details: RequisitesDetails,
    val qrPayload: String
)

class GetBalanceUseCase(private val accountRepository: AccountRepository) {
    operator fun invoke(userId: Long): BigDecimal = accountRepository.getBalance(userId)
}

class GetAccountsUseCase(private val accountRepository: AccountRepository) {
    operator fun invoke(userId: Long): List<BankAccount> = accountRepository.listAccounts(userId)
}

class TransferBetweenAccountsUseCase(private val accountRepository: AccountRepository) {
    operator fun invoke(
        userId: Long,
        fromType: BankAccountType,
        toType: BankAccountType,
        amount: String,
        currency: String
    ): AccountTransferOutcome {
        if (fromType == toType) {
            throw ValidationException("Счета должны отличаться")
        }
        val parsedAmount = InputValidators.parseAmount(amount)
        val normalizedCurrency = InputValidators.normalizeCurrency(currency)
        val result = accountRepository.transferBetweenAccounts(
            userId = userId,
            fromType = fromType,
            toType = toType,
            currency = normalizedCurrency,
            amount = parsedAmount
        )
        return AccountTransferOutcome(result.fromBalance, result.toBalance, normalizedCurrency)
    }
}

class GetCardsUseCase(private val cardRepository: CardRepository) {
    operator fun invoke(userId: Long): List<Card> = cardRepository.listCards(userId)
}

class GetCurrencyRatesUseCase(private val currencyRepository: CurrencyRepository) {
    operator fun invoke(): List<CurrencyRate> {
        val now = Instant.now()
        return currencyRepository.listCurrencyRates().map { rate ->
            rate.copy(
                rateToRub = RateFluctuation.apply(rate.currency, rate.rateToRub, now.toEpochMilli()),
                updatedAtIso = now.toString()
            )
        }
    }
}

/** Обновляет курсы валют в хранилище данными внешнего источника (Yahoo Finance). */
class UpdateRatesUseCase(
    private val exchangeRateSource: ExchangeRateSource,
    private val currencyRepository: CurrencyRepository
) {
    private val tracked = listOf("USD", "EUR", "CNY")

    operator fun invoke() {
        val rates = exchangeRateSource.fetchRatesToRub(tracked)
        rates.forEach { (currency, rate) ->
            if (!currency.equals("RUB", ignoreCase = true)) {
                currencyRepository.upsertRate(currency, rate)
            }
        }
    }
}

class ConvertCurrencyUseCase(private val accountRepository: AccountRepository) {
    operator fun invoke(userId: Long, fromCurrency: String, toCurrency: String, amount: String): CurrencyConversion {
        val from = InputValidators.normalizeCurrency(fromCurrency)
        val to = InputValidators.normalizeCurrency(toCurrency)
        val parsedAmount = InputValidators.parseAmount(amount)
        val result = accountRepository.convertCurrency(userId, from, to, parsedAmount)
        return CurrencyConversion(
            fromCurrency = from,
            toCurrency = to,
            amount = parsedAmount,
            convertedAmount = result.convertedAmount,
            rate = result.rate,
            fromBalance = result.fromBalance,
            toBalance = result.toBalance
        )
    }
}

class GetPaymentRequisitesUseCase(private val requisitesRepository: RequisitesRepository) {
    operator fun invoke(userId: Long): RequisitesWithQr {
        val record = requisitesRepository.getPaymentRequisites(userId)
        val details = RequisitesDetails(
            recipient = RECIPIENT,
            inn = record.inn,
            kpp = record.kpp,
            bik = record.bik,
            account = record.account,
            correspondentAccount = record.correspondentAccount,
            bank = BANK,
            paymentPurpose = PURPOSE_TEMPLATE.format(record.contractNumber),
            contractNumber = record.contractNumber
        )
        return RequisitesWithQr(details, QrPayloadCodec.encode(details))
    }

    private companion object {
        const val RECIPIENT = "ООО «Ромашка»"
        const val BANK = "АО «Тинькофф Банк», г. Москва"
        const val PURPOSE_TEMPLATE = "Превод средст по договору №%s OOO \"Ромашка\" НДС не облагается"
    }
}
