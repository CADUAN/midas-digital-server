package com.midasdigital.server

import com.midasdigital.server.di.AppComponent
import com.midasdigital.server.infrastructure.config.AppConfig
import com.midasdigital.server.infrastructure.persistence.DatabaseFactory
import com.midasdigital.server.presentation.plugins.configureMonitoring
import com.midasdigital.server.presentation.plugins.configureSerialization
import com.midasdigital.server.presentation.plugins.configureStatusPages
import com.midasdigital.server.presentation.routes.midasDigitalRoutes
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun main() {
    val config = AppConfig.fromEnvironment()
    DatabaseFactory.init(config)
    Runtime.getRuntime().addShutdownHook(Thread { DatabaseFactory.close() })

    val component = AppComponent(config)
    startRateUpdates(component)

    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = config.port
    ) {
        module(component)
    }.start(wait = true)
}

/** Раз в 30 секунд тянет котировки с Yahoo Finance и обновляет курсы в БД. */
private fun startRateUpdates(component: AppComponent) {
    val log = LoggerFactory.getLogger("RateUpdater")
    val scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "rate-updater").apply { isDaemon = true }
    }
    scheduler.scheduleWithFixedDelay({
        runCatching { component.updateRatesUseCase() }
            .onFailure { log.warn("Обновление курсов не удалось: {}", it.message) }
    }, 0, 30, TimeUnit.SECONDS)
    Runtime.getRuntime().addShutdownHook(Thread { scheduler.shutdownNow() })
}

fun Application.module(component: AppComponent) {
    configureMonitoring()
    configureSerialization()
    configureStatusPages()

    routing {
        midasDigitalRoutes(component)
    }
}
