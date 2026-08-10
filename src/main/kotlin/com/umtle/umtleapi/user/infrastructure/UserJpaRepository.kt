package com.umtle.umtleapi.user.infrastructure

import com.umtle.umtleapi.user.domain.UserRole
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    @EntityGraph(attributePaths = ["roles"])
    override fun findById(id: Long): Optional<UserJpaEntity>

    @EntityGraph(attributePaths = ["roles"])
    fun findByLoginId(loginId: String): UserJpaEntity?

    fun existsByLoginId(loginId: String): Boolean

    @Query("select count(u) > 0 from UserJpaEntity u join u.roles r where r = :role")
    fun existsByRole(
        @Param("role") role: UserRole,
    ): Boolean
}
