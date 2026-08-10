package com.umtle.umtleapi.user.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface UserRoleJpaRepository : JpaRepository<UserRoleJpaEntity, Long> {
    fun findByUserId(userId: Long): List<UserRoleJpaEntity>
}
