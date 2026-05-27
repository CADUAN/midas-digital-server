package com.midasdigital.server.application.usecase

import com.midasdigital.server.domain.error.NotFoundException
import com.midasdigital.server.domain.error.ValidationException
import com.midasdigital.server.domain.model.PaymentRequisitesRecord
import com.midasdigital.server.domain.model.Transaction
import com.midasdigital.server.domain.model.TransferResult
import com.midasdigital.server.domain.repository.TransactionRepository
import com.midasdigital.server.domain.repository.UserRepository
import com.midasdigital.server.domain.service.InputValidators
import com.midasdigital.server.domain.service.Money
import com.midasdigital.server.domain.service.PhoneNormalizer
import com.midasdigital.server.domain.service.QrPayloadCodec

class TransferUseCase(private val transactionRepository: TransactionRepository) {
    operator fun invoke(userId: Long, recipientPhone: String, amount: String, note: String?): TransferResult {
        val phone = PhoneNormalizer.normalize(recipientPhone)
        val parsedAmount = InputValidators.parseAmount(amount)
        return transactionRepository.transfer(userId, phone, parsedAmount, note.cleaned())
    }
}

class TransferByCardUseCase(
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository
) {
    operator fun invoke(userId: Long, cardNumber: String, amount: String, note: String?): TransferResult {
        val card = InputValidators.normalizeCardNumber(cardNumber)
        val parsedAmount = InputValidators.parseAmount(amount)
        val recipientId = userRepository.findUserIdByCardNumber(card)
            ?: throw NotFoundException("Карта получателя не найдена")
        return transactionRepository.transferByUserId(
            fromUserId = userId,
            recipientUserId = recipientId,
            amount = parsedAmount,
            note = note.cleaned(),
            cardNumber = card
        )
    }
}

class TransferByRequisitesUseCase(
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository
) {
    operator fun invoke(
        userId: Long,
        inn: String,
        kpp: String,
        bik: String,
        account: String,
        correspondentAccount: String,
        contractNumber: String,
        amount: String,
        note: String?
    ): TransferResult {
        val requisites = PaymentRequisitesRecord(
            inn = InputValidators.normalizeDigits(inn, "ИНН"),
            kpp = InputValidators.normalizeDigits(kpp, "КПП"),
            bik = InputValidators.normalizeDigits(bik, "БИК"),
            account = InputValidators.normalizeDigits(account, "Номер счета"),
            correspondentAccount = InputValidators.normalizeDigits(correspondentAccount, "Корр. счет"),
            contractNumber = InputValidators.normalizeDigits(contractNumber, "Номер договора")
        )
        val parsedAmount = InputValidators.parseAmount(amount)
        val recipientId = userRepository.findUserIdByRequisites(requisites)
            ?: throw NotFoundException("Реквизиты получателя не найдены")
        return transactionRepository.transferByUserId(
            fromUserId = userId,
            recipientUserId = recipientId,
            amount = parsedAmount,
            note = note.cleaned(),
            contractNumber = requisites.contractNumber
        )
    }
}

class TransferByQrUseCase(
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository
) {
    operator fun invoke(userId: Long, qrPayload: String, amount: String, note: String?): TransferResult {
        val parsedAmount = InputValidators.parseAmount(amount)
        val qrData = QrPayloadCodec.decode(qrPayload)
        val requisites = PaymentRequisitesRecord(
            inn = InputValidators.normalizeDigits(qrData["inn"], "ИНН"),
            kpp = InputValidators.normalizeDigits(qrData["kpp"], "КПП"),
            bik = InputValidators.normalizeDigits(qrData["bik"], "БИК"),
            account = InputValidators.normalizeDigits(qrData["account"], "Номер счета"),
            correspondentAccount = InputValidators.normalizeDigits(qrData["correspondentAccount"], "Корр. счет"),
            contractNumber = InputValidators.normalizeDigits(qrData["contractNumber"], "Номер договора")
        )
        val recipientId = userRepository.findUserIdByRequisites(requisites)
            ?: throw NotFoundException("QR-код не принадлежит получателю")
        return transactionRepository.transferByUserId(
            fromUserId = userId,
            recipientUserId = recipientId,
            amount = parsedAmount,
            note = note.cleaned(),
            contractNumber = requisites.contractNumber
        )
    }
}

class GetTransactionsUseCase(private val transactionRepository: TransactionRepository) {
    operator fun invoke(userId: Long): List<Transaction> = transactionRepository.listTransactions(userId)
}

class SearchTransactionsUseCase(private val transactionRepository: TransactionRepository) {
    operator fun invoke(userId: Long, rawQuery: String): List<Transaction> {
        val query = rawQuery.trim()
        if (query.isBlank()) {
            throw ValidationException("Поисковый запрос не должен быть пустым")
        }
        val normalizedQuery = query.lowercase()
        return transactionRepository.listTransactions(userId).filter { transaction ->
            transaction.counterpartyPhone.lowercase().contains(normalizedQuery) ||
                Money.format(transaction.amount).contains(normalizedQuery) ||
                transaction.note?.lowercase()?.contains(normalizedQuery) == true ||
                transaction.cardNumber?.lowercase()?.contains(normalizedQuery) == true ||
                transaction.contractNumber?.lowercase()?.contains(normalizedQuery) == true
        }
    }
}

private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
