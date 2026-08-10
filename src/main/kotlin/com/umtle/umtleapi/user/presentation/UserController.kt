package com.umtle.umtleapi.user.presentation

import com.umtle.umtleapi.user.application.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
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
    ): UserResponse = UserResponse.from(userService.createUser(request.loginId, request.password, request.roles))

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long,
    ): UserResponse = UserResponse.from(userService.getUser(id))
}
