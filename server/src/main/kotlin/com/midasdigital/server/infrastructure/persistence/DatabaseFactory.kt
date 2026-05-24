package com.midasdigital.server.infrastructure.persistence

import com.midasdigital.server.infrastructure.config.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import java.sql.Connection

object DatabaseFactory {
    private lateinit var dataSource: HikariDataSource

    fun init(config: AppConfig) {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = config.dbUrl
            username = config.dbUser
            password = config.dbPassword
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            isAutoCommit = true
        }

        dataSource = HikariDataSource(hikariConfig)

        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }

    fun <T> withConnection(block: (Connection) -> T): T {
        return dataSource.connection.use(block)
    }

    fun close() {
        if (::dataSource.isInitialized) {
            dataSource.close()
        }
    }
}
