package com.umtle.umtleapi.user.presentation

import com.umtle.umtleapi.user.application.PendingUserRoleFilter
import com.umtle.umtleapi.user.application.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val userService: UserService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid @RequestBody request: CreateUserRequest,
    ): UserResponse =
        UserResponse.from(
            userService.createUser(
                request.loginId,
                request.password,
                request.roles,
                request.name?.takeIf { it.isNotBlank() } ?: request.loginId,
            ),
        )

    @GetMapping("/pending")
    fun pending(
        @RequestParam role: PendingUserRoleFilter,
    ): List<UserResponse> = userService.listPendingUsers(currentLoginId(), role).map { UserResponse.from(it) }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long,
    ): UserResponse = UserResponse.from(userService.getUser(id))

    @PostMapping("/{id}/approve")
    fun approve(
        @PathVariable id: Long,
    ): UserResponse = UserResponse.from(userService.approveUser(id, currentLoginId()))

    @PostMapping("/{id}/reject")
    fun reject(
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        userService.rejectUser(id, currentLoginId())
        return ResponseEntity.noContent().build()
    }

    private fun currentLoginId(): String = requireNotNull(SecurityContextHolder.getContext().authentication?.name)
}
