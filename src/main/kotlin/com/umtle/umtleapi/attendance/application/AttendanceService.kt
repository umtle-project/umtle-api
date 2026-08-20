package com.umtle.umtleapi.attendance.application

import com.umtle.umtleapi.attendance.domain.Attendance
import com.umtle.umtleapi.attendance.domain.AttendanceNotFoundException
import com.umtle.umtleapi.attendance.domain.AttendanceRepository
import com.umtle.umtleapi.attendance.domain.AttendanceStatus
import com.umtle.umtleapi.classroom.domain.ClassroomRepository
import com.umtle.umtleapi.lesson.domain.LessonNotFoundException
import com.umtle.umtleapi.lesson.domain.LessonRepository
import com.umtle.umtleapi.student.domain.StudentNotFoundException
import com.umtle.umtleapi.student.domain.StudentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AttendanceService(
    private val attendanceRepository: AttendanceRepository,
    private val lessonRepository: LessonRepository,
    private val studentRepository: StudentRepository,
    private val classroomRepository: ClassroomRepository,
) {
    @Transactional
    fun record(
        lessonId: Long,
        studentId: Long,
        status: AttendanceStatus,
    ): Attendance {
        val lesson =
            lessonRepository.findById(lessonId) ?: throw LessonNotFoundException(lessonId)
        if (!studentRepository.existsById(studentId)) {
            throw StudentNotFoundException(studentId)
        }

        if (!classroomRepository.existsStudentAssignment(lesson.classId, studentId)) {
            throw UnassignedAttendanceStudentException(lessonId, studentId)
        }

        if (attendanceRepository.existsByLessonIdAndStudentId(lessonId, studentId)) {
            throw DuplicateAttendanceException(lessonId, studentId)
        }

        return attendanceRepository.save(
            Attendance.record(
                lessonId = lessonId,
                studentId = studentId,
                status = status,
            ),
        )
    }

    @Transactional
    fun updateStatus(
        id: Long,
        newStatus: AttendanceStatus,
    ): Attendance {
        val attendance = findById(id)
        attendance.updateStatus(newStatus)
        return attendanceRepository.save(attendance)
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): Attendance = attendanceRepository.findById(id) ?: throw AttendanceNotFoundException(id)

    @Transactional(readOnly = true)
    fun findAllByLessonId(lessonId: Long): List<Attendance> {
        lessonRepository.findById(lessonId) ?: throw LessonNotFoundException(lessonId)
        return attendanceRepository.findAllByLessonId(lessonId)
    }

    @Transactional(readOnly = true)
    fun findAll(): List<Attendance> = attendanceRepository.findAll()
}
