package com.midasdigital.server.domain.service

/** Порт хеширования ПИН-кода. Реализация (BCrypt) живёт в инфраструктуре. */
interface PasswordHasher {
    fun hash(raw: String): String
    fun verify(raw: String, hash: String): Boolean
}
