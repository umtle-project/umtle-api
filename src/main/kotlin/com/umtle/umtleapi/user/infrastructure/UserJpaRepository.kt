package com.umtle.umtleapi.user.infrastructure

import com.umtle.umtleapi.user.domain.UserRole
import com.umtle.umtleapi.user.domain.UserStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    fun findByLoginId(loginId: String): UserJpaEntity?

    fun existsByLoginId(loginId: String): Boolean

    fun existsByStudentId(studentId: Long): Boolean

    @Query(
        """
        select distinct u
        from UserJpaEntity u
        where u.status = :status
          and exists (
            select 1
            from UserRoleJpaEntity ur
            where ur.userId = u.id and ur.role in :roles
          )
        """,
    )
    fun findByStatusAndRoleIn(
        @Param("status") status: UserStatus,
        @Param("roles") roles: Set<UserRole>,
    ): List<UserJpaEntity>

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
