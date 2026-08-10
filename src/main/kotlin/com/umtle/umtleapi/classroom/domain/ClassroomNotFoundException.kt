package com.umtle.umtleapi.classroom.domain

import com.umtle.umtleapi.common.domain.AggregateNotFoundException

class ClassroomNotFoundException(
    val classId: Long,
) : AggregateNotFoundException("Classroom", classId)
