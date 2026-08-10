package com.umtle.umtleapi.student.domain

import com.umtle.umtleapi.common.domain.AggregateNotFoundException

class StudentNotFoundException(
    val studentId: Long,
) : AggregateNotFoundException("Student", studentId)
