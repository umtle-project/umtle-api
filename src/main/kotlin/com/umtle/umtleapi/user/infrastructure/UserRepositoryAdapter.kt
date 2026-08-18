package com.umtle.umtleapi.user.infrastructure

import com.github.f4b6a3.tsid.TsidCreator
import com.querydsl.core.Tuple
import com.querydsl.jpa.impl.JPAQueryFactory
import com.umtle.umtleapi.user.domain.User
import com.umtle.umtleapi.user.domain.UserRepository
import com.umtle.umtleapi.user.domain.UserRole
import org.springframework.stereotype.Repository

@Repository
class UserRepositoryAdapter(
    private val jpaRepository: UserJpaRepository,
    private val roleJpaRepository: UserRoleJpaRepository,
    private val parentStudentJpaRepository: ParentStudentJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : UserRepository {
    private val user = QUserJpaEntity.userJpaEntity
    private val userRole = QUserRoleJpaEntity.userRoleJpaEntity
    private val parentStudent = QParentStudentJpaEntity.parentStudentJpaEntity

    override fun save(user: User): User {
        jpaRepository.save(user.toEntity())
        syncRoles(user.id, user.roles)
        syncParentStudents(user.id, user.childStudentIds)
        return user
    }

    override fun findById(id: Long): User? =
        findUsers {
            where(user.id.eq(id))
        }.singleOrNull()

    override fun findByLoginId(loginId: String): User? =
        findUsers {
            where(user.loginId.eq(loginId))
        }.singleOrNull()

    override fun existsByLoginId(loginId: String): Boolean = jpaRepository.existsByLoginId(loginId)

    override fun existsById(id: Long): Boolean = jpaRepository.existsById(id)

    override fun existsByIdAndRole(
        id: Long,
        role: UserRole,
    ): Boolean = roleJpaRepository.existsByUserIdAndRole(id, role)

    override fun existsByStudentId(studentId: Long): Boolean = jpaRepository.existsByStudentId(studentId)

    override fun existsByRole(role: UserRole): Boolean = roleJpaRepository.existsByRole(role)

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

    private fun findUsers(applyWhere: com.querydsl.jpa.impl.JPAQuery<Tuple>.() -> Unit): List<User> {
        val query =
            queryFactory
                .select(user, userRole.role, parentStudent.studentId)
                .from(user)
                .leftJoin(userRole)
                .on(userRole.userId.eq(user.id))
                .leftJoin(parentStudent)
                .on(parentStudent.parentUserId.eq(user.id))
        query.applyWhere()
        return query.fetch().toDomains(
            userPath = user,
            rolePath = userRole,
            childStudentIdPath = parentStudent,
        )
    }

    private fun List<Tuple>.toDomains(
        userPath: QUserJpaEntity,
        rolePath: QUserRoleJpaEntity,
        childStudentIdPath: QParentStudentJpaEntity?,
    ): List<User> {
        val rowsByUserId = groupBy { requireNotNull(it.get(userPath)).id }
        return rowsByUserId.values.map { rows ->
            val entity = requireNotNull(rows.first().get(userPath))
            entity.toDomain(
                roles = rows.mapNotNull { it.get(rolePath.role) }.toSet(),
                childStudentIds = childStudentIdPath?.let { path -> rows.mapNotNull { it.get(path.studentId) }.toSet() }.orEmpty(),
            )
        }
    }

    private fun UserJpaEntity.toDomain(
        roles: Set<UserRole>,
        childStudentIds: Set<Long>,
    ) = User.reconstitute(
        id = id,
        loginId = loginId,
        name = name,
        passwordHash = passwordHash,
        roles = roles,
        status = status,
        studentId = studentId,
        childStudentIds = childStudentIds,
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
