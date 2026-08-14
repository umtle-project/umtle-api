package com.umtle.umtleapi.user.domain

import com.github.f4b6a3.tsid.TsidCreator

class User private constructor(
    val id: Long,
    loginId: String,
    name: String,
    passwordHash: String,
    roles: Set<UserRole>,
    status: UserStatus,
    studentId: Long?,
    childStudentIds: Set<Long>,
) {
    var loginId: String = loginId
        private set

    var name: String = name
        private set

    var passwordHash: String = passwordHash
        private set

    var roles: Set<UserRole> = roles
        private set

    var status: UserStatus = status
        private set

    var studentId: Long? = studentId
        private set

    var childStudentIds: Set<Long> = childStudentIds
        private set

    fun deactivate() {
        status = UserStatus.INACTIVE
    }

    fun approve() {
        require(status == UserStatus.PENDING) { "대기 상태의 사용자만 승인할 수 있습니다." }
        status = UserStatus.ACTIVE
    }

    companion object {
        fun register(
            loginId: String,
            name: String,
            passwordHash: String,
            roles: Set<UserRole>,
            studentId: Long? = null,
            childStudentIds: Set<Long> = emptySet(),
        ): User {
            validate(loginId, name, passwordHash, roles, studentId, childStudentIds)
            return User(
                id = TsidCreator.getTsid().toLong(),
                loginId = loginId,
                name = name,
                passwordHash = passwordHash,
                roles = roles,
                status = UserStatus.ACTIVE,
                studentId = studentId,
                childStudentIds = childStudentIds,
            )
        }

        fun signupPending(
            loginId: String,
            name: String,
            passwordHash: String,
            role: UserRole,
            studentId: Long? = null,
            childStudentIds: Set<Long> = emptySet(),
        ): User {
            val roles = setOf(role)
            validate(loginId, name, passwordHash, roles, studentId, childStudentIds)
            return User(
                id = TsidCreator.getTsid().toLong(),
                loginId = loginId,
                name = name,
                passwordHash = passwordHash,
                roles = roles,
                status = UserStatus.PENDING,
                studentId = studentId,
                childStudentIds = childStudentIds,
            )
        }

        fun reconstitute(
            id: Long,
            loginId: String,
            name: String,
            passwordHash: String,
            roles: Set<UserRole>,
            status: UserStatus,
            studentId: Long? = null,
            childStudentIds: Set<Long> = emptySet(),
        ): User {
            validate(loginId, name, passwordHash, roles, studentId, childStudentIds)
            return User(
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

        private fun validate(
            loginId: String,
            name: String,
            passwordHash: String,
            roles: Set<UserRole>,
            studentId: Long?,
            childStudentIds: Set<Long>,
        ) {
            require(loginId.isNotBlank()) { "로그인 식별자는 비어 있을 수 없습니다." }
            require(loginId.length <= MAX_LOGIN_ID_LENGTH) { "로그인 식별자는 50자를 초과할 수 없습니다." }
            require(name.isNotBlank()) { "이름은 비어 있을 수 없습니다." }
            require(name.length <= MAX_NAME_LENGTH) { "이름은 100자를 초과할 수 없습니다." }
            require(passwordHash.isNotBlank()) { "비밀번호 해시는 비어 있을 수 없습니다." }
            require(passwordHash.length <= MAX_PASSWORD_HASH_LENGTH) { "비밀번호 해시는 100자를 초과할 수 없습니다." }
            require(roles.isNotEmpty()) { "사용자는 하나 이상의 역할을 가져야 합니다." }
            require(studentId == null || UserRole.STUDENT in roles) { "학생 연결은 STUDENT 역할에만 설정할 수 있습니다." }
            require(childStudentIds.isEmpty() || UserRole.PARENT in roles) { "자녀 연결은 PARENT 역할에만 설정할 수 있습니다." }
            require(studentId == null || childStudentIds.isEmpty()) { "학생 연결과 자녀 연결은 동시에 설정할 수 없습니다." }
        }

        const val MAX_LOGIN_ID_LENGTH = 50
        const val MAX_NAME_LENGTH = 100
        const val MAX_PASSWORD_HASH_LENGTH = 100
    }
}
