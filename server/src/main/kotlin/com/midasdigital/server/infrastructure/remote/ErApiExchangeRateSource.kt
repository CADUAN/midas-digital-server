package com.midasdigital.server.infrastructure.remote

import com.midasdigital.server.domain.repository.ExchangeRateSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * Источник курсов exchangerate-api (open.er-api.com) — бесплатно, без ключа.
 * Запрос latest/RUB отдаёт, сколько 1 RUB стоит в валюте C; курс к рублю = 1 / rates[C].
 */
class ErApiExchangeRateSource : ExchangeRateSource {

    private val log = LoggerFactory.getLogger(ErApiExchangeRateSource::class.java)
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build()
    private val json = Json { ignoreUnknownKeys = true }

    override fun fetchRatesToRub(currencies: List<String>): Map<String, BigDecimal> {
        val response = runCatching { request() }.getOrElse {
            log.warn("Не удалось получить курсы с er-api: {}", it.message)
            return emptyMap()
        } ?: return emptyMap()

        if (response.result != "success") {
            log.warn("er-api вернул result={}", response.result)
            return emptyMap()
        }

        val result = LinkedHashMap<String, BigDecimal>()
        for (currency in currencies) {
            val code = currency.uppercase()
            if (code == "RUB") {
                result[code] = BigDecimal.ONE
                continue
            }
            val perRub = response.rates[code]
            if (perRub != null && perRub > 0.0) {
                result[code] = BigDecimal.ONE.divide(BigDecimal.valueOf(perRub), 6, RoundingMode.HALF_EVEN)
            }
        }
        return result
    }

    private fun request(): ErApiResponse? {
        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://open.er-api.com/v6/latest/RUB"))
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(8))
            .GET()
            .build()
        val response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            log.warn("er-api статус {}", response.statusCode())
            return null
        }
        return json.decodeFromString<ErApiResponse>(response.body())
    }
}

@Serializable
private data class ErApiResponse(
    val result: String? = null,
    val rates: Map<String, Double> = emptyMap()
)
