package com.midasdigital.server.domain.service

import com.midasdigital.server.domain.error.ValidationException
import java.math.BigDecimal
import java.math.RoundingMode

/** Проверки и нормализация входных значений согласно бизнес-правилам. */
object InputValidators {

    fun parseAmount(rawAmount: String): BigDecimal {
        val parsed = rawAmount.toBigDecimalOrNull()
            ?: throw ValidationException("Сумма должна быть числом")

        val amount = parsed.setScale(2, RoundingMode.HALF_EVEN)
        if (amount <= BigDecimal.ZERO) {
            throw ValidationException("Сумма должна быть больше нуля")
        }
        return amount
    }

    fun validatePin(pin: String) {
        if (!pin.matches(Regex("\\d{4}"))) {
            throw ValidationException("ПИН-код должен состоять из 4 цифр")
        }
    }

    fun normalizeCurrency(rawCurrency: String): String {
        val currency = rawCurrency.trim().uppercase()
        if (!currency.matches(Regex("[A-Z]{3}"))) {
            throw ValidationException("Некорректный код валюты")
        }
        return currency
    }

    fun normalizeCardNumber(rawNumber: String): String {
        val normalized = rawNumber.trim()
        if (!normalized.matches(Regex("\\d{16}"))) {
            throw ValidationException("Номер карты должен содержать 16 цифр")
        }
        return normalized
    }

    fun normalizeDigits(value: String?, fieldName: String): String {
        val normalized = value?.trim().orEmpty()
        if (normalized.isEmpty() || !normalized.matches(Regex("\\d+"))) {
            throw ValidationException("$fieldName должен содержать только цифры")
        }
        return normalized
    }
}
