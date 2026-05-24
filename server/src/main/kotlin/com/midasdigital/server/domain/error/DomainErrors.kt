package com.midasdigital.server.domain.error

/** Базовая доменная ошибка. Слой представления отображает её в HTTP-статус. */
sealed class DomainException(message: String) : RuntimeException(message)

class ValidationException(message: String) : DomainException(message)

class AuthException(message: String) : DomainException(message)

class ConflictException(message: String) : DomainException(message)

class NotFoundException(message: String) : DomainException(message)

class InsufficientFundsException(message: String) : DomainException(message)
