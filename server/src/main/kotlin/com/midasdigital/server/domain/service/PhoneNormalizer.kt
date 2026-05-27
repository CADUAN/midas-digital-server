package com.midasdigital.server.domain.service

import com.midasdigital.server.domain.error.ValidationException

object PhoneNormalizer {
    fun normalize(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        if (digits.length !in 10..15) {
            throw ValidationException("Некорректный номер телефона")
        }
        return "+$digits"
    }
}
