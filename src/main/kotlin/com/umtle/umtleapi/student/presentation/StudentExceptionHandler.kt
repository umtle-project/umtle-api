package com.umtle.umtleapi.student.presentation

import com.umtle.umtleapi.student.domain.StudentNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackageClasses = [StudentController::class])
class StudentExceptionHandler {
    @ExceptionHandler(StudentNotFoundException::class)
    fun handleStudentNotFound(exception: StudentNotFoundException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.message ?: "Student not found")
}
