package com.umtle.umtleapi.attendance.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface AttendanceJpaRepository : JpaRepository<AttendanceJpaEntity, Long> {
    fun findAllByLessonId(lessonId: Long): List<AttendanceJpaEntity>

    fun findAllByStudentIdOrderByCreatedAtDesc(studentId: Long): List<AttendanceJpaEntity>

    fun existsByLessonIdAndStudentId(
        lessonId: Long,
        studentId: Long,
    ): Boolean
}
