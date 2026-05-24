package com.midasdigital.server.infrastructure.config

import java.math.BigDecimal

data class AppConfig(
    val port: Int,
    val dbUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val sessionTtlDays: Long,
    val demoInitialBalance: BigDecimal
) {
    companion object {
        fun fromEnvironment(): AppConfig {
            return AppConfig(
                port = System.getenv("MIDAS_DIGITAL_PORT")?.toIntOrNull() ?: 8080,
                dbUrl = System.getenv("MIDAS_DIGITAL_DB_URL") ?: "jdbc:postgresql://localhost:5433/midas_digital",
                dbUser = System.getenv("MIDAS_DIGITAL_DB_USER") ?: "midas_digital",
                dbPassword = System.getenv("MIDAS_DIGITAL_DB_PASSWORD") ?: "midas_digital",
                sessionTtlDays = System.getenv("MIDAS_DIGITAL_SESSION_TTL_DAYS")?.toLongOrNull() ?: 30L,
                demoInitialBalance = System.getenv("MIDAS_DIGITAL_INITIAL_BALANCE")
                    ?.toBigDecimalOrNull()
                    ?.setScale(2)
                    ?: BigDecimal("1000.00")
            )
        }
    }
}
