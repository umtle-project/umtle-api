package com.umtle.umtleapi.common.domain

import org.springframework.http.HttpStatus

abstract class UmtleCustomException(
    message: String,
    val status: HttpStatus,
) : RuntimeException(message)
