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
    private val parentStudentJpaRepository: ParentStudentJpaRepository,
) : UserRepository {
    override fun save(user: User): User {
        val savedUser = jpaRepository.save(user.toEntity())
        syncRoles(user.id, user.roles)
        syncParentStudents(user.id, user.childStudentIds)
        return savedUser.toDomain()
    }

    override fun findById(id: Long): User? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByLoginId(loginId: String): User? = jpaRepository.findByLoginId(loginId)?.toDomain()

    override fun findPendingByRoles(roles: Set<UserRole>): List<User> =
        jpaRepository.findByStatusAndRoleIn(com.umtle.umtleapi.user.domain.UserStatus.PENDING, roles).map { it.toDomain() }

    override fun existsByLoginId(loginId: String): Boolean = jpaRepository.existsByLoginId(loginId)

    override fun existsByStudentId(studentId: Long): Boolean = jpaRepository.existsByStudentId(studentId)

    override fun existsByRole(role: UserRole): Boolean = jpaRepository.existsByRole(role)

    override fun delete(user: User) {
        roleJpaRepository.deleteAll(roleJpaRepository.findByUserId(user.id))
        parentStudentJpaRepository.deleteAll(parentStudentJpaRepository.findByParentUserId(user.id))
        jpaRepository.deleteById(user.id)
    }

    private fun User.toEntity() =
        UserJpaEntity(
            id = id,
            loginId = loginId,
            name = name,
            passwordHash = passwordHash,
            status = status,
            studentId = studentId,
        )

    private fun UserJpaEntity.toDomain() =
        User.reconstitute(
            id = id,
            loginId = loginId,
            name = name,
            passwordHash = passwordHash,
            roles = roleJpaRepository.findByUserId(id).map { it.role }.toSet(),
            status = status,
            studentId = studentId,
            childStudentIds = parentStudentJpaRepository.findByParentUserId(id).map { it.studentId }.toSet(),
        )

    private fun UserRole.toEntity(userId: Long) =
        UserRoleJpaEntity(
            id = TsidCreator.getTsid().toLong(),
            userId = userId,
            role = this,
        )

    private fun Long.toParentStudentEntity(parentUserId: Long) =
        ParentStudentJpaEntity(
            id = TsidCreator.getTsid().toLong(),
            parentUserId = parentUserId,
            studentId = this,
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

    private fun syncParentStudents(
        parentUserId: Long,
        targetStudentIds: Set<Long>,
    ) {
        val currentLinks = parentStudentJpaRepository.findByParentUserId(parentUserId)
        val currentStudentIds = currentLinks.map { it.studentId }.toSet()

        parentStudentJpaRepository.deleteAll(currentLinks.filter { it.studentId !in targetStudentIds })
        parentStudentJpaRepository.saveAll((targetStudentIds - currentStudentIds).map { it.toParentStudentEntity(parentUserId) })
    }
}
