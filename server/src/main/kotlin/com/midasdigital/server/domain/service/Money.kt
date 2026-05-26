package com.midasdigital.server.domain.service

import java.math.BigDecimal
import java.math.RoundingMode

/** Единый источник правды для форматирования денежных значений и курсов. */
object Money {
    fun format(value: BigDecimal): String =
        value.setScale(2, RoundingMode.HALF_EVEN).toPlainString()

    fun formatRate(value: BigDecimal): String =
        value.setScale(6, RoundingMode.HALF_EVEN).toPlainString()
}
