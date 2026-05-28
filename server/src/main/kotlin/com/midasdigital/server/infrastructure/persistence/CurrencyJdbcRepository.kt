package com.midasdigital.server.infrastructure.persistence

import com.midasdigital.server.domain.model.CurrencyRate
import com.midasdigital.server.domain.repository.CurrencyRepository
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

class CurrencyJdbcRepository : CurrencyRepository {

    override fun listCurrencyRates(): List<CurrencyRate> {
        val sql = """
            SELECT currency, rate_to_rub, updated_at
            FROM currency_rates
            ORDER BY currency
        """.trimIndent()

        return DatabaseFactory.withConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                CurrencyRate(
                                    currency = rs.getString("currency"),
                                    rateToRub = rs.getBigDecimal("rate_to_rub"),
                                    updatedAtIso = rs.getTimestamp("updated_at")
                                        ?.toInstant()
                                        ?.toString()
                                        ?: OffsetDateTime.now(ZoneOffset.UTC).toInstant().toString()
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override fun upsertRate(currency: String, rateToRub: BigDecimal) {
        val sql = """
            INSERT INTO currency_rates (currency, rate_to_rub, updated_at)
            VALUES (?, ?, NOW())
            ON CONFLICT (currency) DO UPDATE
            SET rate_to_rub = EXCLUDED.rate_to_rub,
                updated_at = NOW()
        """.trimIndent()

        DatabaseFactory.withConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, currency.uppercase())
                statement.setBigDecimal(2, rateToRub)
                statement.executeUpdate()
            }
        }
    }
}
