package com.midasdigital.server.domain.service

import com.midasdigital.server.domain.error.ValidationException
import com.midasdigital.server.domain.model.RequisitesDetails

/** Кодирование/декодирование реквизитов в строку QR-кода (key=value|...). */
object QrPayloadCodec {

    private val requiredKeys = listOf("inn", "kpp", "bik", "account", "correspondentAccount", "contractNumber")

    fun encode(requisites: RequisitesDetails): String {
        val fields = listOf(
            "recipient" to requisites.recipient,
            "inn" to requisites.inn,
            "kpp" to requisites.kpp,
            "bik" to requisites.bik,
            "account" to requisites.account,
            "correspondentAccount" to requisites.correspondentAccount,
            "bank" to requisites.bank,
            "paymentPurpose" to requisites.paymentPurpose,
            "contractNumber" to requisites.contractNumber
        )
        return fields.joinToString(separator = "|") { (key, value) -> "$key=${value.trim()}" }
    }

    fun decode(payload: String): Map<String, String> {
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) {
            throw ValidationException("QR-код пустой")
        }
        return trimmed.split("|")
            .mapNotNull { part ->
                val pieces = part.split("=", limit = 2)
                if (pieces.size != 2) null else pieces[0].trim() to pieces[1].trim()
            }
            .toMap()
            .also { map ->
                if (requiredKeys.any { it !in map }) {
                    throw ValidationException("QR-код не содержит реквизиты")
                }
            }
    }
}
