package com.umtle.umtleapi.common.presentation

import com.umtle.umtleapi.common.domain.UmtleCustomException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(UmtleCustomException::class)
    fun handleUmtleCustomException(exception: UmtleCustomException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(exception.status, exception.message ?: "Unexpected error")

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(exception: AccessDeniedException): Nothing = throw exception

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(exception: AuthenticationException): Nothing = throw exception

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ProblemDetail {
        log.error("Unhandled exception during request", exception)
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
        )
    }
}
