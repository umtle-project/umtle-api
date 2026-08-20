package com.umtle.umtleapi.user.application

import com.umtle.umtleapi.common.domain.UmtleCustomException
import org.springframework.http.HttpStatus

class InvalidSignupRequestException(
    message: String,
) : UmtleCustomException(message, HttpStatus.BAD_REQUEST)
