package com.umtle.umtleapi.user.presentation

import com.umtle.umtleapi.user.domain.User
import com.umtle.umtleapi.user.domain.UserRole
import com.umtle.umtleapi.user.domain.UserStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

data class CreateUserRequest(
    @field:NotBlank
    @field:Size(max = User.MAX_LOGIN_ID_LENGTH)
    val loginId: String,
    @field:NotBlank
    val password: String,
    @field:NotEmpty
    val roles: Set<UserRole>,
)

data class LoginRequest(
    @field:NotBlank
    val loginId: String,
    @field:NotBlank
    val password: String,
)

data class UserResponse(
    val id: Long,
    val loginId: String,
    val roles: Set<UserRole>,
    val status: UserStatus,
) {
    companion object {
        fun from(user: User) =
            UserResponse(
                id = user.id,
                loginId = user.loginId,
                roles = user.roles,
                status = user.status,
            )
    }
}
