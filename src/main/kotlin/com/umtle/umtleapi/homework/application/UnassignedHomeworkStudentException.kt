package com.umtle.umtleapi.homework.application

class UnassignedHomeworkStudentException(
    lessonId: Long,
    studentId: Long,
) : RuntimeException("Student $studentId is not assigned to the class for lesson $lessonId")
