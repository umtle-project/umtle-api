package com.umtle.umtleapi.user.infrastructure

import com.github.f4b6a3.tsid.TsidCreator
import com.umtle.umtleapi.user.domain.User
import com.umtle.umtleapi.user.domain.UserRepository
import com.umtle.umtleapi.user.domain.UserRole
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryAdapter(
    private val jpaRepository: UserJpaRepository,
    private val roleJpaRepository: UserRoleJpaRepository,
) : UserRepository {
    override fun save(user: User): User {
        val savedUser = jpaRepository.save(user.toEntity())
        syncRoles(user.id, user.roles)
        return savedUser.toDomain()
    }

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
        )

    private fun UserJpaEntity.toDomain() =
        User.reconstitute(
            id = id,
            loginId = loginId,
            passwordHash = passwordHash,
            roles = roleJpaRepository.findByUserId(id).map { it.role }.toSet(),
            status = status,
        )

    private fun UserRole.toEntity(userId: Long) =
        UserRoleJpaEntity(
            id = TsidCreator.getTsid().toLong(),
            userId = userId,
            role = this,
        )

    private fun syncRoles(
        userId: Long,
        targetRoles: Set<UserRole>,
    ) {
        val currentRoles = roleJpaRepository.findByUserId(userId)
        val targetRoleSet = targetRoles.toSet()
        val currentRoleSet = currentRoles.map { it.role }.toSet()

        roleJpaRepository.deleteAll(currentRoles.filter { it.role !in targetRoleSet })
        roleJpaRepository.saveAll((targetRoleSet - currentRoleSet).map { it.toEntity(userId) })
    }
}
