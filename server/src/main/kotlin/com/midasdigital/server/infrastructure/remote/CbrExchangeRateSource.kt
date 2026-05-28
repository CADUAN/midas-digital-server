package com.midasdigital.server.infrastructure.remote

import com.midasdigital.server.domain.repository.ExchangeRateSource
import kotlinx.serialization.SerialName
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
 * Источник курсов Банка России (cbr-xml-daily.ru) — бесплатно, без ключа, без блокировок.
 * Для валюты XXX: курс к рублю = Value / Nominal.
 */
class CbrExchangeRateSource : ExchangeRateSource {

    private val log = LoggerFactory.getLogger(CbrExchangeRateSource::class.java)
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build()
    private val json = Json { ignoreUnknownKeys = true }

    override fun fetchRatesToRub(currencies: List<String>): Map<String, BigDecimal> {
        val response = runCatching { request() }.getOrElse {
            log.warn("Не удалось получить курсы ЦБ РФ: {}", it.message)
            return emptyMap()
        } ?: return emptyMap()

        val result = LinkedHashMap<String, BigDecimal>()
        for (currency in currencies) {
            val code = currency.uppercase()
            if (code == "RUB") {
                result[code] = BigDecimal.ONE
                continue
            }
            val valute = response.valute[code]
            if (valute != null && valute.nominal > 0 && valute.value > 0.0) {
                result[code] = BigDecimal.valueOf(valute.value)
                    .divide(BigDecimal.valueOf(valute.nominal.toLong()), 6, RoundingMode.HALF_EVEN)
            }
        }
        return result
    }

    private fun request(): CbrResponse? {
        val httpRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://www.cbr-xml-daily.ru/daily_json.js"))
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(8))
            .GET()
            .build()
        val response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            log.warn("ЦБ РФ статус {}", response.statusCode())
            return null
        }
        return json.decodeFromString<CbrResponse>(response.body())
    }
}

@Serializable
private data class CbrResponse(
    @SerialName("Valute") val valute: Map<String, CbrValute> = emptyMap()
)

@Serializable
private data class CbrValute(
    @SerialName("Nominal") val nominal: Int = 1,
    @SerialName("Value") val value: Double = 0.0
)
