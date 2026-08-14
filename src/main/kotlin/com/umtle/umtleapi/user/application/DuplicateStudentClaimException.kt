package com.umtle.umtleapi.user.application

class DuplicateStudentClaimException(
    studentId: Long,
) : RuntimeException("Student is already claimed: $studentId")
