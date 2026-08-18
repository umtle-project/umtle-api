package com.umtle.umtleapi.user.infrastructure

import com.umtle.umtleapi.user.domain.UserRole
import org.springframework.data.jpa.repository.JpaRepository

interface UserRoleJpaRepository : JpaRepository<UserRoleJpaEntity, Long> {
    fun findByUserId(userId: Long): List<UserRoleJpaEntity>

    fun existsByUserIdAndRole(
        userId: Long,
        role: UserRole,
    ): Boolean

    fun existsByRole(role: UserRole): Boolean
}
