package com.umtle.umtleapi.attendance.infrastructure

import com.umtle.umtleapi.attendance.domain.Attendance
import com.umtle.umtleapi.attendance.domain.AttendanceRepository
import org.springframework.stereotype.Repository

@Repository
class AttendanceRepositoryAdapter(
    private val jpaRepository: AttendanceJpaRepository,
) : AttendanceRepository {
    override fun save(attendance: Attendance): Attendance = jpaRepository.save(attendance.toEntity()).toDomain()

    override fun findById(id: Long): Attendance? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAllByLessonId(lessonId: Long): List<Attendance> = jpaRepository.findAllByLessonId(lessonId).map { it.toDomain() }

    override fun findAllByStudentId(studentId: Long): List<Attendance> =
        jpaRepository.findAllByStudentIdOrderByCreatedAtDesc(studentId).map { it.toDomain() }

    override fun findAll(): List<Attendance> = jpaRepository.findAll().map { it.toDomain() }

    override fun existsByLessonIdAndStudentId(
        lessonId: Long,
        studentId: Long,
    ): Boolean = jpaRepository.existsByLessonIdAndStudentId(lessonId, studentId)

    private fun Attendance.toEntity() =
        AttendanceJpaEntity(
            id = id,
            lessonId = lessonId,
            studentId = studentId,
            status = status,
        )

    private fun AttendanceJpaEntity.toDomain() =
        Attendance.reconstitute(
            id = id,
            lessonId = lessonId,
            studentId = studentId,
            status = status,
        )
}
