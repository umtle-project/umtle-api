package com.umtle.umtleapi.user.presentation

import com.umtle.umtleapi.user.application.AuthService
import com.umtle.umtleapi.user.application.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val userService: UserService,
) {
    private val securityContextRepository = HttpSessionSecurityContextRepository()

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse,
    ): UserResponse {
        val user = authService.authenticate(request.loginId, request.password)
        val authentication =
            UsernamePasswordAuthenticationToken.authenticated(
                user.loginId,
                null,
                user.roles.map { SimpleGrantedAuthority("ROLE_${it.name}") },
            )
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)
        securityContextRepository.saveContext(context, servletRequest, servletResponse)
        return UserResponse.from(user)
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    fun signup(
        @Valid @RequestBody request: SignupRequest,
    ): UserResponse =
        UserResponse.from(
            userService.signup(
                loginId = request.loginId,
                password = request.password,
                name = request.name,
                role = request.role,
                studentId = request.studentId,
            ),
        )

    @GetMapping("/me")
    fun me(): UserResponse {
        val loginId = SecurityContextHolder.getContext().authentication?.name ?: throw BadCredentialsException("Bad credentials")
        return UserResponse.from(authService.currentUser(loginId))
    }

    @PostMapping("/logout")
    fun logout(servletRequest: HttpServletRequest) {
        SecurityContextHolder.clearContext()
        servletRequest.getSession(false)?.invalidate()
    }

    @ExceptionHandler(BadCredentialsException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleBadCredentials() {
    }
}
