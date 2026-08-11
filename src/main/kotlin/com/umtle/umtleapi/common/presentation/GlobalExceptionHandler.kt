package com.umtle.umtleapi.common.presentation

import com.umtle.umtleapi.attendance.application.DuplicateAttendanceException
import com.umtle.umtleapi.attendance.application.UnassignedAttendanceStudentException
import com.umtle.umtleapi.classroom.application.InvalidTeacherAssignmentException
import com.umtle.umtleapi.common.domain.AggregateNotFoundException
import com.umtle.umtleapi.lesson.domain.InvalidLessonStateTransitionException
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

    @ExceptionHandler(InvalidLessonStateTransitionException::class)
    fun handleInvalidLessonStateTransition(exception: InvalidLessonStateTransitionException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.message ?: "Invalid lesson state transition")

    @ExceptionHandler(InvalidTeacherAssignmentException::class)
    fun handleInvalidTeacherAssignment(exception: InvalidTeacherAssignmentException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.message ?: "Invalid teacher assignment")

    @ExceptionHandler(DuplicateAttendanceException::class)
    fun handleDuplicateAttendance(exception: DuplicateAttendanceException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.message ?: "Duplicate attendance")

    @ExceptionHandler(UnassignedAttendanceStudentException::class)
    fun handleUnassignedAttendanceStudent(exception: UnassignedAttendanceStudentException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.message ?: "Unassigned attendance student")
}
