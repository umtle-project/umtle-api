package com.umtle.umtleapi.user.domain

interface UserRepository {
    fun save(user: User): User

    fun findById(id: Long): User?

    fun findByLoginId(loginId: String): User?

    fun existsByLoginId(loginId: String): Boolean

    fun existsByRole(role: UserRole): Boolean
}
