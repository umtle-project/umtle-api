package com.umtle.umtleapi.user.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class UserTest {
    @Test
    fun `register creates an active user`() {
        val user = User.register("admin", "관리자", "hashed-password", setOf(UserRole.ADMIN))

        assertEquals("admin", user.loginId)
        assertEquals("관리자", user.name)
        assertEquals("hashed-password", user.passwordHash)
        assertEquals(setOf(UserRole.ADMIN), user.roles)
        assertEquals(UserStatus.ACTIVE, user.status)
    }

    @Test
    fun `register rejects a blank loginId`() {
        assertFailsWith<IllegalArgumentException> {
            User.register("", "관리자", "hashed-password", setOf(UserRole.ADMIN))
        }
    }

    @Test
    fun `register requires at least one role`() {
        assertFailsWith<IllegalArgumentException> {
            User.register("admin", "관리자", "hashed-password", emptySet())
        }
    }

    @Test
    fun `approve activates a pending user`() {
        val user = User.signupPending("teacher", "선생님", "hashed-password", UserRole.TEACHER)

        user.approve()

        assertEquals(UserStatus.ACTIVE, user.status)
    }

    @Test
    fun `reconstitute restores persisted state`() {
        val user =
            User.reconstitute(
                id = 1L,
                loginId = "teacher",
                name = "선생님",
                passwordHash = "hashed-password",
                roles = setOf(UserRole.TEACHER),
                status = UserStatus.INACTIVE,
            )

        assertEquals(1L, user.id)
        assertEquals("teacher", user.loginId)
        assertEquals("선생님", user.name)
        assertEquals(setOf(UserRole.TEACHER), user.roles)
        assertEquals(UserStatus.INACTIVE, user.status)
    }
}
