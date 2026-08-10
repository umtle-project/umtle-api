package com.umtle.umtleapi.user.domain

import com.github.f4b6a3.tsid.TsidCreator

class User private constructor(
    val id: Long,
    loginId: String,
    passwordHash: String,
    roles: Set<UserRole>,
    status: UserStatus,
) {
    var loginId: String = loginId
        private set

    var passwordHash: String = passwordHash
        private set

    var roles: Set<UserRole> = roles
        private set

    var status: UserStatus = status
        private set

    fun deactivate() {
        status = UserStatus.INACTIVE
    }

    companion object {
        fun register(
            loginId: String,
            passwordHash: String,
            roles: Set<UserRole>,
        ): User {
            validate(loginId, passwordHash, roles)
            return User(
                id = TsidCreator.getTsid().toLong(),
                loginId = loginId,
                passwordHash = passwordHash,
                roles = roles,
                status = UserStatus.ACTIVE,
            )
        }

        fun reconstitute(
            id: Long,
            loginId: String,
            passwordHash: String,
            roles: Set<UserRole>,
            status: UserStatus,
        ): User {
            validate(loginId, passwordHash, roles)
            return User(
                id = id,
                loginId = loginId,
                passwordHash = passwordHash,
                roles = roles,
                status = status,
            )
        }

        private fun validate(
            loginId: String,
            passwordHash: String,
            roles: Set<UserRole>,
        ) {
            require(loginId.isNotBlank()) { "로그인 식별자는 비어 있을 수 없습니다." }
            require(loginId.length <= MAX_LOGIN_ID_LENGTH) { "로그인 식별자는 50자를 초과할 수 없습니다." }
            require(passwordHash.isNotBlank()) { "비밀번호 해시는 비어 있을 수 없습니다." }
            require(passwordHash.length <= MAX_PASSWORD_HASH_LENGTH) { "비밀번호 해시는 100자를 초과할 수 없습니다." }
            require(roles.isNotEmpty()) { "사용자는 하나 이상의 역할을 가져야 합니다." }
        }

        const val MAX_LOGIN_ID_LENGTH = 50
        const val MAX_PASSWORD_HASH_LENGTH = 100
    }
}
