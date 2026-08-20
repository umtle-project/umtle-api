package com.umtle.umtleapi.attendance.application

import com.umtle.umtleapi.common.domain.UmtleCustomException
import org.springframework.http.HttpStatus

class DuplicateAttendanceException(
    lessonId: Long,
    studentId: Long,
) : UmtleCustomException(
        "Attendance already exists for lesson $lessonId and student $studentId",
        HttpStatus.BAD_REQUEST,
    )
