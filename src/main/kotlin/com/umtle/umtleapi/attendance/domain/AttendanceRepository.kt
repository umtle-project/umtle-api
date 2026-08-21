package com.umtle.umtleapi.attendance.domain

interface AttendanceRepository {
    fun save(attendance: Attendance): Attendance

    fun findById(id: Long): Attendance?

    fun findAllByLessonId(lessonId: Long): List<Attendance>

    fun findAllByStudentId(studentId: Long): List<Attendance>

    fun findAll(): List<Attendance>

    fun existsByLessonIdAndStudentId(
        lessonId: Long,
        studentId: Long,
    ): Boolean
}
