package com.umtle.umtleapi.student.domain

class StudentNotFoundException(
    val studentId: Long,
) : RuntimeException("Student not found: $studentId")
