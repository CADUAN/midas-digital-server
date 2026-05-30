package com.midasdigital.server.presentation.routes

import com.midasdigital.server.di.AppComponent
import com.midasdigital.server.domain.error.AuthException
import com.midasdigital.server.domain.error.ValidationException
import com.midasdigital.server.presentation.dto.AccountTransferRequest
import com.midasdigital.server.presentation.dto.CurrencyConvertRequest
import com.midasdigital.server.presentation.dto.LoginRequest
import com.midasdigital.server.presentation.dto.RegisterRequest
import com.midasdigital.server.presentation.dto.TransferByCardRequest
import com.midasdigital.server.presentation.dto.TransferByQrRequest
import com.midasdigital.server.presentation.dto.TransferByRequisitesRequest
import com.midasdigital.server.presentation.dto.TransferRequest
import com.midasdigital.server.presentation.mapper.toAccountsResponse
import com.midasdigital.server.presentation.mapper.toBalanceResponse
import com.midasdigital.server.presentation.mapper.toBankAccountType
import com.midasdigital.server.presentation.mapper.toCardsResponse
import com.midasdigital.server.presentation.mapper.toRatesResponse
import com.midasdigital.server.presentation.mapper.toResponse
import com.midasdigital.server.presentation.mapper.toTransactionsResponse
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.midasDigitalRoutes(component: AppComponent) {
    route("/api/v1") {
        post("/auth/register") {
            val request = call.receive<RegisterRequest>()
            val result = component.registerUserUseCase(request.fullName, request.phone, request.pin)
            call.respond(result.toResponse())
        }

        post("/auth/login") {
            val request = call.receive<LoginRequest>()
            val result = component.loginUseCase(request.phone, request.pin)
            call.respond(result.toResponse())
        }

        get("/wallet/balance") {
            val userId = component.authorizeUseCase(call.bearerToken()).id
            call.respond(component.getBalanceUseCase(userId).toBalanceResponse())
        }

        get("/wallet/requisites") {
            val userId = component.authorizeUseCase(call.bearerToken()).id
            call.respond(component.getPaymentRequisitesUseCase(userId).toResponse())
        }

        get("/wallet/rates") {
            component.authorizeUseCase(call.bearerToken())
            call.respond(component.getCurrencyRatesUseCase().toRatesResponse())
        }

        post("/wallet/convert") {
            val userId = component.authorizeUseCase(call.bearerToken()).id
            val request = call.receive<CurrencyConvertRequest>()
            val result = component.convertCurrencyUseCase(
                userId = userId,
                fromCurrency = request.fromCurrency,
                toCurrency = request.toCurrency,
                amount = request.amount
            )
            call.respond(result.toResponse())
        }

        get("/accounts") {
            val userId = component.authorizeUseCase(call.bearerToken()).id
            call.respond(component.getAccountsUseCase(userId).toAccountsResponse())
        }

        post("/accounts/transfer") {
            val userId = component.authorizeUseCase(call.bearerToken()).id
            val request = call.receive<AccountTransferRequest>()
            val result = component.transferBetweenAccountsUseCase(
                userId = userId,
                fromType = request.fromType.toBankAccountType(),
                toType = request.toType.toBankAccountType(),
                amount = request.amount,
                currency = request.currency
            )
            call.respond(result.toResponse())
        }

        get("/cards") {
            val userId = component.authorizeUseCase(call.bearerToken()).id
            call.respond(component.getCardsUseCase(userId).toCardsResponse())
        }

        post("/transactions/transfer") {
            val userId = component.authorizeUseCase(call.bearerToken()).id
            val request = call.receive<TransferRequest>()
            val result = component.transferUseCase(userId, request.recipientPhone, request.amount, request.note)
            call.respond(result.toResponse())
        }

        post("/transactions/transfer/card") {
            val userId = component.authorizeUseCase(call.bearerToken()).id
            val request = call.receive<TransferByCardRequest>()
            val result = component.transferByCardUseCase(userId, request.cardNumber, request.amount, request.note)
            call.respond(result.toResponse())
        }

        post("/transactions/transfer/requisites") {
            val userId = component.authorizeUseCase(call.bearerToken()).id
            val request = call.receive<TransferByRequisitesRequest>()
            val result = component.transferByRequisitesUseCase(
                userId = userId,
                inn = request.inn,
                kpp = request.kpp,
                bik = request.bik,
                account = request.account,
                correspondentAccount = request.correspondentAccount,
                contractNumber = request.contractNumber,
                amount = request.amount,
                note = request.note
            )
            call.respond(result.toResponse())
        }

        post("/transactions/transfer/qr") {
            val userId = component.authorizeUseCase(call.bearerToken()).id
            val request = call.receive<TransferByQrRequest>()
            val result = component.transferByQrUseCase(userId, request.qrPayload, request.amount, request.note)
            call.respond(result.toResponse())
        }

        get("/transactions") {
            val userId = component.authorizeUseCase(call.bearerToken()).id
            call.respond(component.getTransactionsUseCase(userId).toTransactionsResponse())
        }

        get("/transactions/search") {
            val userId = component.authorizeUseCase(call.bearerToken()).id
            val query = call.request.queryParameters["query"]
                ?: throw ValidationException("Отсутствует параметр query")
            call.respond(component.searchTransactionsUseCase(userId, query).toTransactionsResponse())
        }
    }
}

private fun ApplicationCall.bearerToken(): String {
    val authorization = request.headers["Authorization"] ?: throw AuthException("Требуется заголовок Authorization")
    if (!authorization.startsWith("Bearer ")) {
        throw AuthException("Некорректный токен авторизации")
    }
    return authorization.removePrefix("Bearer ").trim()
}
