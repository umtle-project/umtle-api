package com.umtle.umtleapi.common.presentation

import com.umtle.umtleapi.common.domain.AggregateNotFoundException
import com.umtle.umtleapi.user.application.DuplicateLoginIdException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(AggregateNotFoundException::class)
    fun handleAggregateNotFound(exception: AggregateNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.message ?: "Aggregate not found")

    @ExceptionHandler(DuplicateLoginIdException::class)
    fun handleDuplicateLoginId(exception: DuplicateLoginIdException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.message ?: "Duplicate loginId")
}
