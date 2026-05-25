package com.midasdigital.server.application.usecase

import com.midasdigital.server.domain.error.AuthException
import com.midasdigital.server.domain.error.ConflictException
import com.midasdigital.server.domain.error.ValidationException
import com.midasdigital.server.domain.model.SessionUser
import com.midasdigital.server.domain.repository.SessionRepository
import com.midasdigital.server.domain.repository.UserRepository
import com.midasdigital.server.domain.service.InputValidators
import com.midasdigital.server.domain.service.PasswordHasher
import com.midasdigital.server.domain.service.PhoneNormalizer

/** Результат успешной аутентификации. */
data class AuthResult(
    val token: String,
    val user: SessionUser
)

class RegisterUserUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val passwordHasher: PasswordHasher
) {
    operator fun invoke(fullName: String, phone: String, pin: String): AuthResult {
        val name = fullName.trim()
        if (name.length < 2) {
            throw ValidationException("Имя должно содержать минимум 2 символа")
        }

        val normalizedPhone = PhoneNormalizer.normalize(phone)
        InputValidators.validatePin(pin)

        if (userRepository.findUserByPhone(normalizedPhone) != null) {
            throw ConflictException("Пользователь с таким телефоном уже существует")
        }

        val user = userRepository.createUser(name, normalizedPhone, passwordHasher.hash(pin))
        val token = sessionRepository.createSession(user.id)
        return AuthResult(token, SessionUser(user.id, user.fullName, user.phone))
    }
}

class LoginUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val passwordHasher: PasswordHasher
) {
    operator fun invoke(phone: String, pin: String): AuthResult {
        val normalizedPhone = PhoneNormalizer.normalize(phone)
        InputValidators.validatePin(pin)

        val user = userRepository.findUserByPhone(normalizedPhone)
            ?: throw AuthException("Неверный телефон или ПИН-код")

        if (!passwordHasher.verify(pin, user.pinHash)) {
            throw AuthException("Неверный телефон или ПИН-код")
        }

        val token = sessionRepository.createSession(user.id)
        return AuthResult(token, SessionUser(user.id, user.fullName, user.phone))
    }
}

class AuthorizeUseCase(private val sessionRepository: SessionRepository) {
    operator fun invoke(bearerToken: String?): SessionUser {
        val token = bearerToken?.trim().orEmpty()
        if (token.isBlank()) {
            throw AuthException("Требуется токен доступа")
        }
        return sessionRepository.findUserByToken(token)
            ?: throw AuthException("Сессия истекла или недействительна")
    }
}
