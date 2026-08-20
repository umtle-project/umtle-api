package com.umtle.umtleapi.homework.application

import com.umtle.umtleapi.common.domain.UmtleCustomException
import org.springframework.http.HttpStatus

class UnassignedHomeworkStudentException(
    lessonId: Long,
    studentId: Long,
) : UmtleCustomException(
        "Student $studentId is not assigned to the class for lesson $lessonId",
        HttpStatus.BAD_REQUEST,
    )
