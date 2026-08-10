package com.umtle.umtleapi.user.infrastructure

import com.umtle.umtleapi.user.domain.UserRole
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional

interface UserJpaRepository : JpaRepository<UserJpaEntity, Long> {
    // roles는 지연 로딩 값 컬렉션이지만 User 도메인 변환과 인증에서 항상 필요하므로 명시적으로 함께 조회한다.
    @EntityGraph(attributePaths = ["roles"])
    override fun findById(id: Long): Optional<UserJpaEntity>

    // JPA 연관관계 eager loading이 아니라 User가 소유한 값 컬렉션의 LazyInitializationException 방지 목적이다.
    @EntityGraph(attributePaths = ["roles"])
    fun findByLoginId(loginId: String): UserJpaEntity?

    fun existsByLoginId(loginId: String): Boolean

    @Query("select count(u) > 0 from UserJpaEntity u join u.roles r where r = :role")
    fun existsByRole(
        @Param("role") role: UserRole,
    ): Boolean
}
