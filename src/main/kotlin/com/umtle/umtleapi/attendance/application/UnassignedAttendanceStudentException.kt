package com.umtle.umtleapi.attendance.application

class UnassignedAttendanceStudentException(
    lessonId: Long,
    studentId: Long,
) : RuntimeException("Student $studentId is not assigned to the class for lesson $lessonId")
