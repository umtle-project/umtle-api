package com.umtle.umtleapi.user.infrastructure

import com.umtle.umtleapi.user.domain.User
import com.umtle.umtleapi.user.domain.UserRepository
import com.umtle.umtleapi.user.domain.UserRole
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryAdapter(
    private val jpaRepository: UserJpaRepository,
) : UserRepository {
    override fun save(user: User): User = jpaRepository.save(user.toEntity()).toDomain()

    override fun findById(id: Long): User? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByLoginId(loginId: String): User? = jpaRepository.findByLoginId(loginId)?.toDomain()

    override fun existsByLoginId(loginId: String): Boolean = jpaRepository.existsByLoginId(loginId)

    override fun existsByRole(role: UserRole): Boolean = jpaRepository.existsByRole(role)

    private fun User.toEntity() =
        UserJpaEntity(
            id = id,
            loginId = loginId,
            passwordHash = passwordHash,
            status = status,
            roles = roles.toMutableSet(),
        )

    private fun UserJpaEntity.toDomain() =
        User.reconstitute(
            id = id,
            loginId = loginId,
            passwordHash = passwordHash,
            roles = roles.toSet(),
            status = status,
        )
}
