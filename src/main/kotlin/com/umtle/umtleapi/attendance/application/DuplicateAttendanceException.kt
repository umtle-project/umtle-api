package com.umtle.umtleapi.attendance.application

class DuplicateAttendanceException(
    lessonId: Long,
    studentId: Long,
) : RuntimeException("Attendance already exists for lesson $lessonId and student $studentId")
