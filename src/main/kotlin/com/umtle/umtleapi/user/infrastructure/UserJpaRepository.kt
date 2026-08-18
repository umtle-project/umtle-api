package com.umtle.umtleapi.user.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    fun existsByLoginId(loginId: String): Boolean

    fun existsByStudentId(studentId: Long): Boolean
}
