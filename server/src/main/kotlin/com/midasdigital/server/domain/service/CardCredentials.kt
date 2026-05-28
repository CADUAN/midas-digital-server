package com.midasdigital.server.domain.service

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Деривация платёжных реквизитов карты: срок действия — 5 лет с момента выпуска
 * (карта выпускается при регистрации), CVV — детерминированный 3-значный код.
 */
object CardCredentials {
    private const val VALIDITY_YEARS = 5L
    private val expiryFormatter = DateTimeFormatter.ofPattern("MM/yy")

    fun expiry(issuedAt: OffsetDateTime): String =
        issuedAt.plusYears(VALIDITY_YEARS).format(expiryFormatter)

    fun cvv(cardNumber: String): String {
        val code = ((cardNumber.hashCode() % 1000) + 1000) % 1000
        return code.toString().padStart(3, '0')
    }
}
