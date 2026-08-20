package com.umtle.umtleapi.user.application

import com.umtle.umtleapi.common.domain.UmtleCustomException
import org.springframework.http.HttpStatus

class DuplicateLoginIdException(
    loginId: String,
) : UmtleCustomException("Duplicate loginId: $loginId", HttpStatus.CONFLICT)
