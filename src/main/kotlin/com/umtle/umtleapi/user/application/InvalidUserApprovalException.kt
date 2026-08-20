package com.umtle.umtleapi.user.application

import com.umtle.umtleapi.common.domain.UmtleCustomException
import org.springframework.http.HttpStatus

class InvalidUserApprovalException(
    message: String,
) : UmtleCustomException(message, HttpStatus.CONFLICT)
