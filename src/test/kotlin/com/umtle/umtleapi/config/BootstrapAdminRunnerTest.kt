package com.umtle.umtleapi.config

import com.umtle.umtleapi.user.application.UserService
import com.umtle.umtleapi.user.domain.User
import com.umtle.umtleapi.user.domain.UserRepository
import com.umtle.umtleapi.user.domain.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class BootstrapAdminRunnerTest {
    @Test
    fun `creates an admin user when none exists`() {
        val repository = InMemoryUserRepository()
        val runner =
            BootstrapAdminRunner(
                userRepository = repository,
                userService = UserService(repository, BCryptPasswordEncoder()),
                loginId = "admin",
                password = "admin-password",
            )

        runner.run(TestApplicationArguments)

        val admin = repository.findByLoginId("admin")
        assertEquals(setOf(UserRole.ADMIN), admin?.roles)
        assertTrue(BCryptPasswordEncoder().matches("admin-password", admin?.passwordHash))
    }

    @Test
    fun `does not create another admin when one already exists`() {
        val repository = InMemoryUserRepository()
        repository.save(User.register("existing-admin", "hashed-password", setOf(UserRole.ADMIN)))
        val runner =
            BootstrapAdminRunner(
                userRepository = repository,
                userService = UserService(repository, BCryptPasswordEncoder()),
                loginId = "new-admin",
                password = "admin-password",
            )

        runner.run(TestApplicationArguments)

        assertEquals(1, repository.users.size)
        assertEquals("existing-admin", repository.users.single().loginId)
    }

    private object TestApplicationArguments : org.springframework.boot.ApplicationArguments {
        override fun getSourceArgs(): Array<String> = emptyArray()

        override fun getOptionNames(): Set<String> = emptySet()

        override fun containsOption(name: String): Boolean = false

        override fun getOptionValues(name: String): List<String>? = null

        override fun getNonOptionArgs(): List<String> = emptyList()
    }

    private class InMemoryUserRepository : UserRepository {
        val users = mutableListOf<User>()

        override fun save(user: User): User {
            users.removeIf { it.id == user.id }
            users.add(user)
            return user
        }

        override fun findById(id: Long): User? = users.firstOrNull { it.id == id }

        override fun findByLoginId(loginId: String): User? = users.firstOrNull { it.loginId == loginId }

        override fun existsByLoginId(loginId: String): Boolean = users.any { it.loginId == loginId }

        override fun existsByRole(role: UserRole): Boolean = users.any { role in it.roles }
    }
}
