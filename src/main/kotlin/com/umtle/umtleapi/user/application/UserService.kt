package com.umtle.umtleapi.user.application

import com.umtle.umtleapi.user.domain.User
import com.umtle.umtleapi.user.domain.UserNotFoundException
import com.umtle.umtleapi.user.domain.UserRepository
import com.umtle.umtleapi.user.domain.UserRole
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun createUser(
        loginId: String,
        password: String,
        roles: Set<UserRole>,
    ): User {
        if (userRepository.existsByLoginId(loginId)) {
            throw DuplicateLoginIdException(loginId)
        }

        return userRepository.save(
            User.register(
                loginId = loginId,
                passwordHash = requireNotNull(passwordEncoder.encode(password)),
                roles = roles,
            ),
        )
    }

    @Transactional(readOnly = true)
    fun getUser(id: Long): User = userRepository.findById(id) ?: throw UserNotFoundException(id)
}
