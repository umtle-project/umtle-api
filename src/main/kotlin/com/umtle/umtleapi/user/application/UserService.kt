package com.umtle.umtleapi.user.application

import com.umtle.umtleapi.student.domain.StudentNotFoundException
import com.umtle.umtleapi.student.domain.StudentRepository
import com.umtle.umtleapi.user.domain.PendingUserQuery
import com.umtle.umtleapi.user.domain.User
import com.umtle.umtleapi.user.domain.UserNotFoundException
import com.umtle.umtleapi.user.domain.UserRepository
import com.umtle.umtleapi.user.domain.UserRole
import com.umtle.umtleapi.user.domain.UserStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val pendingUserQuery: PendingUserQuery,
    private val studentRepository: StudentRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional
    fun createUser(
        loginId: String,
        password: String,
        roles: Set<UserRole>,
        name: String = loginId,
    ): User {
        if (userRepository.existsByLoginId(loginId)) {
            throw DuplicateLoginIdException(loginId)
        }

        return userRepository.save(
            User.register(
                loginId = loginId,
                name = name,
                passwordHash = requireNotNull(passwordEncoder.encode(password)),
                roles = roles,
            ),
        )
    }

    @Transactional
    fun signup(
        loginId: String,
        password: String,
        name: String,
        role: UserRole,
        studentId: Long?,
    ): User {
        if (role == UserRole.ADMIN) {
            throw InvalidSignupRequestException("ADMIN은 자가 회원가입할 수 없습니다.")
        }
        if (role == UserRole.TEACHER && studentId != null) {
            throw InvalidSignupRequestException("TEACHER 가입에는 studentId를 설정할 수 없습니다.")
        }
        if (role in setOf(UserRole.STUDENT, UserRole.PARENT) && studentId == null) {
            throw InvalidSignupRequestException("${role.name} 가입에는 studentId가 필요합니다.")
        }
        if (userRepository.existsByLoginId(loginId)) {
            throw DuplicateLoginIdException(loginId)
        }

        val claimedStudentId =
            studentId?.also {
                if (!studentRepository.existsById(it)) {
                    throw StudentNotFoundException(it)
                }
            }

        if (role == UserRole.STUDENT && claimedStudentId != null && userRepository.existsByStudentId(claimedStudentId)) {
            throw DuplicateStudentClaimException(claimedStudentId)
        }

        val childStudentIds = if (role == UserRole.PARENT && claimedStudentId != null) setOf(claimedStudentId) else emptySet()

        return userRepository.save(
            User.signupPending(
                loginId = loginId,
                name = name,
                passwordHash = requireNotNull(passwordEncoder.encode(password)),
                role = role,
                studentId = if (role == UserRole.STUDENT) claimedStudentId else null,
                childStudentIds = childStudentIds,
            ),
        )
    }

    @Transactional(readOnly = true)
    fun getUser(id: Long): User = userRepository.findById(id) ?: throw UserNotFoundException(id)

    @Transactional(readOnly = true)
    fun listPendingUsers(
        currentLoginId: String,
        role: PendingUserRoleFilter,
    ): List<User> {
        val (requiredRoles, pendingRoles) =
            when (role) {
                PendingUserRoleFilter.TEACHER -> setOf(UserRole.ADMIN) to setOf(UserRole.TEACHER)
                PendingUserRoleFilter.STUDENT_PARENT -> setOf(UserRole.ADMIN, UserRole.TEACHER) to setOf(UserRole.STUDENT, UserRole.PARENT)
            }

        return pendingUserQuery.findForApprover(
            loginId = currentLoginId,
            approverRoles = requiredRoles,
            pendingRoles = pendingRoles,
        ) ?: throw AccessDeniedException("Access denied")
    }

    @Transactional
    fun approveUser(
        id: Long,
        currentLoginId: String,
    ): User {
        val currentUser = currentUser(currentLoginId)
        val target = getUser(id)
        requireApprovalPermission(currentUser, target)
        target.approve()
        return userRepository.save(target)
    }

    @Transactional
    fun rejectUser(
        id: Long,
        currentLoginId: String,
    ) {
        val currentUser = currentUser(currentLoginId)
        val target = getUser(id)
        requireApprovalPermission(currentUser, target)
        userRepository.delete(target)
    }

    private fun currentUser(loginId: String): User = userRepository.findByLoginId(loginId) ?: throw AccessDeniedException("Access denied")

    private fun requireApprovalPermission(
        currentUser: User,
        target: User,
    ) {
        if (target.status != UserStatus.PENDING) {
            throw InvalidUserApprovalException("대기 상태의 사용자만 승인 또는 거절할 수 있습니다.")
        }
        when {
            UserRole.TEACHER in target.roles -> requireAdmin(currentUser)
            target.roles.any { it == UserRole.STUDENT || it == UserRole.PARENT } -> requireAdminOrTeacher(currentUser)
            else -> throw AccessDeniedException("Access denied")
        }
    }

    private fun requireAdmin(user: User) {
        if (UserRole.ADMIN !in user.roles || user.status != UserStatus.ACTIVE) {
            throw AccessDeniedException("Access denied")
        }
    }

    private fun requireAdminOrTeacher(user: User) {
        if (user.status != UserStatus.ACTIVE || (UserRole.ADMIN !in user.roles && UserRole.TEACHER !in user.roles)) {
            throw AccessDeniedException("Access denied")
        }
    }
}

enum class PendingUserRoleFilter {
    TEACHER,
    STUDENT_PARENT,
}
