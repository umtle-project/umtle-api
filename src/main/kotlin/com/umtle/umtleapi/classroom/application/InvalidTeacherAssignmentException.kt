package com.umtle.umtleapi.classroom.application

import com.umtle.umtleapi.common.domain.UmtleCustomException
import org.springframework.http.HttpStatus

class InvalidTeacherAssignmentException(
    teacherId: Long,
) : UmtleCustomException("User is not a teacher: $teacherId", HttpStatus.BAD_REQUEST)
