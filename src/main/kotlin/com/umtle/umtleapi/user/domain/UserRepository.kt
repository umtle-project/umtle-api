package com.umtle.umtleapi.user.domain

interface UserRepository {
    fun save(user: User): User

    fun findById(id: Long): User?

    fun findByLoginId(loginId: String): User?

    fun existsById(id: Long): Boolean

    fun existsByIdAndRole(
        id: Long,
        role: UserRole,
    ): Boolean

    fun existsByLoginId(loginId: String): Boolean

    fun existsByStudentId(studentId: Long): Boolean

    fun existsByRole(role: UserRole): Boolean

    fun delete(user: User)
}
