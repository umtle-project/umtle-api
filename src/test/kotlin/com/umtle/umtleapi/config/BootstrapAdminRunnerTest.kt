package com.umtle.umtleapi.config

import com.umtle.umtleapi.student.domain.Student
import com.umtle.umtleapi.student.domain.StudentRepository
import com.umtle.umtleapi.user.application.UserService
import com.umtle.umtleapi.user.domain.PendingUserQuery
import com.umtle.umtleapi.user.domain.User
import com.umtle.umtleapi.user.domain.UserRepository
import com.umtle.umtleapi.user.domain.UserRole
import com.umtle.umtleapi.user.domain.UserStatus
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
                userService =
                    UserService(
                        repository,
                        InMemoryPendingUserQuery(repository),
                        EmptyStudentRepository,
                        BCryptPasswordEncoder(),
                    ),
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
        repository.save(User.register("existing-admin", "기존 관리자", "hashed-password", setOf(UserRole.ADMIN)))
        val runner =
            BootstrapAdminRunner(
                userRepository = repository,
                userService =
                    UserService(
                        repository,
                        InMemoryPendingUserQuery(repository),
                        EmptyStudentRepository,
                        BCryptPasswordEncoder(),
                    ),
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

        override fun existsById(id: Long): Boolean = users.any { it.id == id }

        override fun existsByIdAndRole(
            id: Long,
            role: UserRole,
        ): Boolean = users.any { it.id == id && role in it.roles }

        override fun existsByLoginId(loginId: String): Boolean = users.any { it.loginId == loginId }

        override fun existsByStudentId(studentId: Long): Boolean = users.any { it.studentId == studentId }

        override fun existsByRole(role: UserRole): Boolean = users.any { role in it.roles }

        override fun delete(user: User) {
            users.removeIf { it.id == user.id }
        }
    }

    private class InMemoryPendingUserQuery(
        private val repository: InMemoryUserRepository,
    ) : PendingUserQuery {
        override fun findForApprover(
            loginId: String,
            approverRoles: Set<UserRole>,
            pendingRoles: Set<UserRole>,
        ): List<User>? {
            val currentUser = repository.users.firstOrNull { it.loginId == loginId } ?: return null
            if (currentUser.status != UserStatus.ACTIVE || currentUser.roles.none { it in approverRoles }) {
                return null
            }
            return repository.users.filter { it.status == UserStatus.PENDING && it.roles.any { role -> role in pendingRoles } }
        }
    }

    private object EmptyStudentRepository : StudentRepository {
        override fun save(student: Student): Student = student

        override fun findById(id: Long): Student? = null

        override fun existsById(id: Long): Boolean = false

        override fun findAll(): List<Student> = emptyList()

        override fun searchByName(name: String): List<Student> = emptyList()
    }
}
