package com.umtle.umtleapi.user.application

import com.umtle.umtleapi.common.domain.UmtleCustomException
import org.springframework.http.HttpStatus

class DuplicateStudentClaimException(
    studentId: Long,
) : UmtleCustomException("Student is already claimed: $studentId", HttpStatus.CONFLICT)
