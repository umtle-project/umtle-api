package com.umtle.umtleapi.classroom.application

class InvalidTeacherAssignmentException(
    teacherId: Long,
) : RuntimeException("User is not a teacher: $teacherId")
