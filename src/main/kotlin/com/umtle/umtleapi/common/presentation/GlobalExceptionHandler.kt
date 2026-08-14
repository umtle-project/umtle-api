package com.umtle.umtleapi.common.presentation

import com.umtle.umtleapi.attendance.application.DuplicateAttendanceException
import com.umtle.umtleapi.attendance.application.UnassignedAttendanceStudentException
import com.umtle.umtleapi.classroom.application.InvalidTeacherAssignmentException
import com.umtle.umtleapi.common.domain.AggregateNotFoundException
import com.umtle.umtleapi.homework.application.InvalidHomeworkUpdateException
import com.umtle.umtleapi.homework.application.UnassignedHomeworkStudentException
import com.umtle.umtleapi.homework.presentation.InvalidHomeworkRequestException
import com.umtle.umtleapi.lesson.domain.InvalidLessonStateTransitionException
import com.umtle.umtleapi.user.application.DuplicateLoginIdException
import com.umtle.umtleapi.user.application.DuplicateStudentClaimException
import com.umtle.umtleapi.user.application.InvalidSignupRequestException
import com.umtle.umtleapi.user.application.InvalidUserApprovalException
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

    @ExceptionHandler(DuplicateStudentClaimException::class)
    fun handleDuplicateStudentClaim(exception: DuplicateStudentClaimException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.message ?: "Duplicate student claim")

    @ExceptionHandler(InvalidSignupRequestException::class)
    fun handleInvalidSignupRequest(exception: InvalidSignupRequestException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.message ?: "Invalid signup request")

    @ExceptionHandler(InvalidUserApprovalException::class)
    fun handleInvalidUserApproval(exception: InvalidUserApprovalException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.message ?: "Invalid user approval")

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

    @ExceptionHandler(UnassignedHomeworkStudentException::class)
    fun handleUnassignedHomeworkStudent(exception: UnassignedHomeworkStudentException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.message ?: "Unassigned homework student")

    @ExceptionHandler(InvalidHomeworkUpdateException::class)
    fun handleInvalidHomeworkUpdate(exception: InvalidHomeworkUpdateException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.message ?: "Invalid homework update")

    @ExceptionHandler(InvalidHomeworkRequestException::class)
    fun handleInvalidHomeworkRequest(exception: InvalidHomeworkRequestException): ProblemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.message ?: "Invalid homework request")
}
