package com.umtle.umtleapi.config

import com.umtle.umtleapi.user.domain.UserRepository
import com.umtle.umtleapi.user.domain.UserStatus
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UmtleUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByLoginId(username) ?: throw UsernameNotFoundException(username)
        return User(
            user.loginId,
            user.passwordHash,
            user.status == UserStatus.ACTIVE,
            true,
            true,
            true,
            user.roles.map { SimpleGrantedAuthority("ROLE_${it.name}") },
        )
    }
}
