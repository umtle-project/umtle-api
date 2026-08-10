package com.umtle.umtleapi.user.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class UserTest {
    @Test
    fun `register creates an active user`() {
        val user = User.register("admin", "hashed-password", setOf(UserRole.ADMIN))

        assertEquals("admin", user.loginId)
        assertEquals("hashed-password", user.passwordHash)
        assertEquals(setOf(UserRole.ADMIN), user.roles)
        assertEquals(UserStatus.ACTIVE, user.status)
    }

    @Test
    fun `register rejects a blank loginId`() {
        assertFailsWith<IllegalArgumentException> {
            User.register("", "hashed-password", setOf(UserRole.ADMIN))
        }
    }

    @Test
    fun `register requires at least one role`() {
        assertFailsWith<IllegalArgumentException> {
            User.register("admin", "hashed-password", emptySet())
        }
    }

    @Test
    fun `reconstitute restores persisted state`() {
        val user =
            User.reconstitute(
                id = 1L,
                loginId = "teacher",
                passwordHash = "hashed-password",
                roles = setOf(UserRole.TEACHER),
                status = UserStatus.INACTIVE,
            )

        assertEquals(1L, user.id)
        assertEquals("teacher", user.loginId)
        assertEquals(setOf(UserRole.TEACHER), user.roles)
        assertEquals(UserStatus.INACTIVE, user.status)
    }
}
