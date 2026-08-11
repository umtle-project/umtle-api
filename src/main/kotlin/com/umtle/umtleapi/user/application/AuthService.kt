package com.umtle.umtleapi.user.application

import com.umtle.umtleapi.user.domain.User
import com.umtle.umtleapi.user.domain.UserRepository
import com.umtle.umtleapi.user.domain.UserStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional(readOnly = true)
    fun authenticate(
        loginId: String,
        password: String,
    ): User {
        val user = userRepository.findByLoginId(loginId) ?: throw BadCredentialsException("Bad credentials")
        if (user.status != UserStatus.ACTIVE || !passwordEncoder.matches(password, user.passwordHash)) {
            throw BadCredentialsException("Bad credentials")
        }
        return user
    }

    @Transactional(readOnly = true)
    fun currentUser(loginId: String): User = userRepository.findByLoginId(loginId) ?: throw BadCredentialsException("Bad credentials")
}
