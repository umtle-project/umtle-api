package com.umtle.umtleapi.lesson.domain

import com.umtle.umtleapi.common.domain.UmtleCustomException
import org.springframework.http.HttpStatus

class InvalidLessonStateTransitionException(
    action: String,
    status: LessonStatus,
) : UmtleCustomException("Cannot $action a lesson in $status status", HttpStatus.BAD_REQUEST)
