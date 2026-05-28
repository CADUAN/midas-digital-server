package com.midasdigital.server.domain.service

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.sin

/**
 * Лёгкая «живая» рябь поверх реального курса: к базовому значению применяется
 * плавное колебание во времени, своё для каждой валюты, чтобы курс заметно
 * двигался между опросами. Рубль (база) не меняется.
 */
object RateFluctuation {
    private const val AMPLITUDE = 0.008 // ±0.8%

    fun apply(currency: String, base: BigDecimal, atEpochMillis: Long): BigDecimal {
        if (currency.equals("RUB", ignoreCase = true)) return base
        val phase = currency.hashCode().toDouble()
        val factor = 1.0 + AMPLITUDE * sin(atEpochMillis / 15000.0 + phase)
        return base.multiply(BigDecimal(factor)).setScale(6, RoundingMode.HALF_EVEN)
    }
}
