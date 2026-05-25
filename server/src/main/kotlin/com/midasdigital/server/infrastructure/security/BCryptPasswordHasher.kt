package com.midasdigital.server.infrastructure.security

import com.midasdigital.server.domain.service.PasswordHasher
import org.mindrot.jbcrypt.BCrypt

/** Реализация хеширования ПИН-кода на BCrypt. */
class BCryptPasswordHasher : PasswordHasher {
    override fun hash(raw: String): String = BCrypt.hashpw(raw, BCrypt.gensalt())
    override fun verify(raw: String, hash: String): Boolean = BCrypt.checkpw(raw, hash)
}
