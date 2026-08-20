package com.umtle.umtleapi.homework.application

import com.umtle.umtleapi.common.domain.UmtleCustomException
import org.springframework.http.HttpStatus

class InvalidHomeworkUpdateException(
    message: String,
) : UmtleCustomException(message, HttpStatus.BAD_REQUEST)
