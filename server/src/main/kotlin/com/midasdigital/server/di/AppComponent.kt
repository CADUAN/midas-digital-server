package com.midasdigital.server.di

import com.midasdigital.server.application.usecase.AuthorizeUseCase
import com.midasdigital.server.application.usecase.ConvertCurrencyUseCase
import com.midasdigital.server.application.usecase.GetAccountsUseCase
import com.midasdigital.server.application.usecase.GetBalanceUseCase
import com.midasdigital.server.application.usecase.GetCardsUseCase
import com.midasdigital.server.application.usecase.GetCurrencyRatesUseCase
import com.midasdigital.server.application.usecase.GetPaymentRequisitesUseCase
import com.midasdigital.server.application.usecase.GetTransactionsUseCase
import com.midasdigital.server.application.usecase.LoginUseCase
import com.midasdigital.server.application.usecase.RegisterUserUseCase
import com.midasdigital.server.application.usecase.SearchTransactionsUseCase
import com.midasdigital.server.application.usecase.TransferBetweenAccountsUseCase
import com.midasdigital.server.application.usecase.TransferByCardUseCase
import com.midasdigital.server.application.usecase.TransferByQrUseCase
import com.midasdigital.server.application.usecase.TransferByRequisitesUseCase
import com.midasdigital.server.application.usecase.TransferUseCase
import com.midasdigital.server.application.usecase.UpdateRatesUseCase
import com.midasdigital.server.infrastructure.config.AppConfig
import com.midasdigital.server.infrastructure.remote.CbrExchangeRateSource
import com.midasdigital.server.infrastructure.remote.CompositeExchangeRateSource
import com.midasdigital.server.infrastructure.remote.ErApiExchangeRateSource
import com.midasdigital.server.infrastructure.persistence.AccountJdbcRepository
import com.midasdigital.server.infrastructure.persistence.CardJdbcRepository
import com.midasdigital.server.infrastructure.persistence.CurrencyJdbcRepository
import com.midasdigital.server.infrastructure.persistence.RequisitesJdbcRepository
import com.midasdigital.server.infrastructure.persistence.SessionJdbcRepository
import com.midasdigital.server.infrastructure.persistence.TransactionJdbcRepository
import com.midasdigital.server.infrastructure.persistence.UserJdbcRepository
import com.midasdigital.server.infrastructure.security.BCryptPasswordHasher

/**
 * Корень композиции: связывает инфраструктуру, доменные сервисы и use case'ы.
 * Слой представления получает только готовые use case'ы.
 */
class AppComponent(config: AppConfig) {
    // Инфраструктура (адаптеры портов)
    private val userRepository = UserJdbcRepository(config)
    private val sessionRepository = SessionJdbcRepository(config)
    private val accountRepository = AccountJdbcRepository()
    private val cardRepository = CardJdbcRepository()
    private val requisitesRepository = RequisitesJdbcRepository()
    private val currencyRepository = CurrencyJdbcRepository()
    private val transactionRepository = TransactionJdbcRepository()
    private val passwordHasher = BCryptPasswordHasher()
    // Основной источник — Банк России (ЦБ РФ); er-api оставлен как резерв.
    private val exchangeRateSource = CompositeExchangeRateSource(
        listOf(CbrExchangeRateSource(), ErApiExchangeRateSource())
    )

    // Auth
    val registerUserUseCase = RegisterUserUseCase(userRepository, sessionRepository, passwordHasher)
    val loginUseCase = LoginUseCase(userRepository, sessionRepository, passwordHasher)
    val authorizeUseCase = AuthorizeUseCase(sessionRepository)

    // Wallet
    val getBalanceUseCase = GetBalanceUseCase(accountRepository)
    val getAccountsUseCase = GetAccountsUseCase(accountRepository)
    val transferBetweenAccountsUseCase = TransferBetweenAccountsUseCase(accountRepository)
    val getCardsUseCase = GetCardsUseCase(cardRepository)
    val getCurrencyRatesUseCase = GetCurrencyRatesUseCase(currencyRepository)
    val updateRatesUseCase = UpdateRatesUseCase(exchangeRateSource, currencyRepository)
    val convertCurrencyUseCase = ConvertCurrencyUseCase(accountRepository)
    val getPaymentRequisitesUseCase = GetPaymentRequisitesUseCase(requisitesRepository)

    // Transactions
    val transferUseCase = TransferUseCase(transactionRepository)
    val transferByCardUseCase = TransferByCardUseCase(transactionRepository, userRepository)
    val transferByRequisitesUseCase = TransferByRequisitesUseCase(transactionRepository, userRepository)
    val transferByQrUseCase = TransferByQrUseCase(transactionRepository, userRepository)
    val getTransactionsUseCase = GetTransactionsUseCase(transactionRepository)
    val searchTransactionsUseCase = SearchTransactionsUseCase(transactionRepository)
}
