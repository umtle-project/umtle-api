package com.umtle.umtleapi.attendance.application

import com.umtle.umtleapi.common.domain.UmtleCustomException
import org.springframework.http.HttpStatus

class UnassignedAttendanceStudentException(
    lessonId: Long,
    studentId: Long,
) : UmtleCustomException(
        "Student $studentId is not assigned to the class for lesson $lessonId",
        HttpStatus.BAD_REQUEST,
    )
