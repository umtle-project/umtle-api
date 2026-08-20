package com.umtle.umtleapi.homework.presentation

import com.umtle.umtleapi.common.domain.UmtleCustomException
import org.springframework.http.HttpStatus

class InvalidHomeworkRequestException(
    message: String,
) : UmtleCustomException(message, HttpStatus.BAD_REQUEST)
