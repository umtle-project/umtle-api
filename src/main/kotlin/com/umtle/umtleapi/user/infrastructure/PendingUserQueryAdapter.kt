package com.umtle.umtleapi.user.infrastructure

import com.querydsl.core.Tuple
import com.querydsl.jpa.JPAExpressions
import com.querydsl.jpa.impl.JPAQueryFactory
import com.umtle.umtleapi.user.domain.PendingUserQuery
import com.umtle.umtleapi.user.domain.User
import com.umtle.umtleapi.user.domain.UserRole
import com.umtle.umtleapi.user.domain.UserStatus
import org.springframework.stereotype.Repository

@Repository
class PendingUserQueryAdapter(
    private val queryFactory: JPAQueryFactory,
) : PendingUserQuery {
    override fun findForApprover(
        loginId: String,
        approverRoles: Set<UserRole>,
        pendingRoles: Set<UserRole>,
    ): List<User>? {
        val currentUser = QUserJpaEntity("currentUser")
        val currentRole = QUserRoleJpaEntity("currentRole")
        val pendingUser = QUserJpaEntity("pendingUser")
        val pendingUserRole = QUserRoleJpaEntity("pendingUserRole")
        val pendingParentStudent = QParentStudentJpaEntity("pendingParentStudent")
        val filterPendingRole = QUserRoleJpaEntity("filterPendingRole")
        val includeChildStudentIds = UserRole.PARENT in pendingRoles

        val rows =
            if (includeChildStudentIds) {
                queryFactory
                    .select(pendingUser, pendingUserRole.role, pendingParentStudent.studentId)
                    .from(currentUser)
                    .join(currentRole)
                    .on(currentRole.userId.eq(currentUser.id).and(currentRole.role.`in`(approverRoles)))
                    .leftJoin(pendingUser)
                    .on(pendingUser.pendingWithAnyRole(filterPendingRole, pendingRoles))
                    .leftJoin(pendingUserRole)
                    .on(pendingUserRole.userId.eq(pendingUser.id))
                    .leftJoin(pendingParentStudent)
                    .on(pendingParentStudent.parentUserId.eq(pendingUser.id))
                    .where(currentUser.loginId.eq(loginId).and(currentUser.status.eq(UserStatus.ACTIVE)))
                    .fetch()
            } else {
                queryFactory
                    .select(pendingUser, pendingUserRole.role)
                    .from(currentUser)
                    .join(currentRole)
                    .on(currentRole.userId.eq(currentUser.id).and(currentRole.role.`in`(approverRoles)))
                    .leftJoin(pendingUser)
                    .on(pendingUser.pendingWithAnyRole(filterPendingRole, pendingRoles))
                    .leftJoin(pendingUserRole)
                    .on(pendingUserRole.userId.eq(pendingUser.id))
                    .where(currentUser.loginId.eq(loginId).and(currentUser.status.eq(UserStatus.ACTIVE)))
                    .fetch()
            }

        if (rows.isEmpty()) {
            return null
        }

        return rows
            .filter { it.get(pendingUser) != null }
            .toUsers(
                userPath = pendingUser,
                rolePath = pendingUserRole,
                childStudentIdPath = pendingParentStudent.takeIf { includeChildStudentIds },
            )
    }

    private fun QUserJpaEntity.pendingWithAnyRole(
        filterRole: QUserRoleJpaEntity,
        roles: Set<UserRole>,
    ) = status
        .eq(UserStatus.PENDING)
        .and(
            JPAExpressions
                .selectOne()
                .from(filterRole)
                .where(filterRole.userId.eq(id).and(filterRole.role.`in`(roles)))
                .exists(),
        )

    private fun List<Tuple>.toUsers(
        userPath: QUserJpaEntity,
        rolePath: QUserRoleJpaEntity,
        childStudentIdPath: QParentStudentJpaEntity?,
    ): List<User> =
        groupBy { requireNotNull(it.get(userPath)).id }
            .values
            .map { rows ->
                val entity = requireNotNull(rows.first().get(userPath))
                entity.toDomain(
                    roles = rows.mapNotNull { it.get(rolePath.role) }.toSet(),
                    childStudentIds = childStudentIdPath?.let { path -> rows.mapNotNull { it.get(path.studentId) }.toSet() }.orEmpty(),
                )
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
}
