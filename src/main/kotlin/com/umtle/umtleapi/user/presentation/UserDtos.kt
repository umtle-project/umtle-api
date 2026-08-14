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
    @field:Size(max = User.MAX_NAME_LENGTH)
    val name: String?,
    @field:NotEmpty
    val roles: Set<UserRole>,
)

data class SignupRequest(
    @field:NotBlank
    @field:Size(max = User.MAX_LOGIN_ID_LENGTH)
    val loginId: String,
    @field:NotBlank
    val password: String,
    @field:NotBlank
    @field:Size(max = User.MAX_NAME_LENGTH)
    val name: String,
    val role: UserRole,
    val studentId: Long?,
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
    val name: String,
    val roles: Set<UserRole>,
    val status: UserStatus,
    val studentId: Long?,
    val childStudentIds: Set<Long>,
) {
    companion object {
        fun from(user: User) =
            UserResponse(
                id = user.id,
                loginId = user.loginId,
                name = user.name,
                roles = user.roles,
                status = user.status,
                studentId = user.studentId,
                childStudentIds = user.childStudentIds,
            )
    }
}
