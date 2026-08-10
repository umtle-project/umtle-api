package com.umtle.umtleapi.config

import com.umtle.umtleapi.user.application.UserService
import com.umtle.umtleapi.user.domain.UserRepository
import com.umtle.umtleapi.user.domain.UserRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class BootstrapAdminRunner(
    private val userRepository: UserRepository,
    private val userService: UserService,
    @Value("\${umtle.bootstrap-admin.login-id:}") private val loginId: String,
    @Value("\${umtle.bootstrap-admin.password:}") private val password: String,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return
        }

        require(loginId.isNotBlank()) { "umtle.bootstrap-admin.login-id must be configured when no ADMIN user exists." }
        require(password.isNotBlank()) { "umtle.bootstrap-admin.password must be configured when no ADMIN user exists." }

        userService.createUser(loginId, password, setOf(UserRole.ADMIN))
    }
}
