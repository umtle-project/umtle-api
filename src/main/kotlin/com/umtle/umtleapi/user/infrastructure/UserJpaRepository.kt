package com.umtle.umtleapi.user.infrastructure

import com.umtle.umtleapi.user.domain.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    fun findByLoginId(loginId: String): UserJpaEntity?

    fun existsByLoginId(loginId: String): Boolean

    @Query(
        """
        select count(u) > 0
        from UserJpaEntity u
        where exists (
            select 1
            from UserRoleJpaEntity ur
            where ur.userId = u.id and ur.role = :role
        )
        """,
    )
    fun existsByRole(
        @Param("role") role: UserRole,
    ): Boolean
}
