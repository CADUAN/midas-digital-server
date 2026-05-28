package com.midasdigital.server.infrastructure.remote

import com.midasdigital.server.domain.repository.ExchangeRateSource
import java.math.BigDecimal

/**
 * Перебирает источники по порядку и возвращает первый, отдавший реальные курсы
 * (есть хотя бы одна валюта кроме RUB).
 */
class CompositeExchangeRateSource(
    private val sources: List<ExchangeRateSource>
) : ExchangeRateSource {

    override fun fetchRatesToRub(currencies: List<String>): Map<String, BigDecimal> {
        for (source in sources) {
            val rates = runCatching { source.fetchRatesToRub(currencies) }.getOrElse { emptyMap() }
            if (rates.keys.any { !it.equals("RUB", ignoreCase = true) }) {
                return rates
            }
        }
        return emptyMap()
    }
}
